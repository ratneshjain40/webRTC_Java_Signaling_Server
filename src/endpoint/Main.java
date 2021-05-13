package endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Signaling server to WebRTC video conferencing.
 */
@ServerEndpoint("/signal")
public class Main {

	static final SessionManager session_manager = new SessionManager();

	@OnOpen
	public void whenOpening(Session session) throws IOException, EncodeException {
		System.out.println("Open! " + session.getId());
	}

	@OnMessage
	public void process(String data, Session session) throws IOException {
		System.out.println("Got signal - " + data);
		JSONObject client_obj = new JSONObject(data);

		String action = client_obj.getString("action");
		String room_id = client_obj.getString("room_id");

		if (action.equals("Create Room")) {

			JSONObject data_obj = client_obj.getJSONObject("data");
			String username = data_obj.getString("username");

			if (session_manager.room_exists(room_id)) {
				JSONObject res_obj = session_manager.create_response("room already exists");

				session.getBasicRemote().sendText(res_obj.toString());
				System.out.println(room_id + " Exists !");
			} else {
				session_manager.create_room(username, room_id, session);
				JSONObject res_obj = session_manager.create_response("room created");

				session.getBasicRemote().sendText(res_obj.toString());
			}
		}

		if (action.equals("Join Room")) {

			JSONObject data_obj = client_obj.getJSONObject("data");
			String username = data_obj.getString("username");

			// room exists -> not in room already -> if username is unique then join room.
			if (session_manager.room_exists(room_id)) {
				if (!session_manager.is_in_room(room_id, session)) {
					ArrayList<String> participants = session_manager.get_participants_username(room_id);
					if (!participants.contains(username)) {
						session_manager.join_room(username, room_id, session);

						// here send room participants to client
						JSONObject res_obj = new JSONObject();
						JSONObject data_temp = new JSONObject();

						data_temp.put("type", "participants");
						data_temp.put("username", new JSONArray(participants));

						res_obj.put("response", "room joined");
						res_obj.put("data", data_temp.toString());

						session.getBasicRemote().sendText(res_obj.toString());
					} else {
						JSONObject res_obj = session_manager.create_response("username taken");
						session.getBasicRemote().sendText(res_obj.toString());
					}

				}
			} else {
				JSONObject res_obj = session_manager.create_response("room does not exist");

				session.getBasicRemote().sendText(res_obj.toString());
				System.out.println(room_id + "Does not Exist !");
			}
		}

		if (action.equals("Send Data")) {

			String to_user = client_obj.getString("to_user");
			JSONObject data_obj = client_obj.getJSONObject("data");

			if (session_manager.is_in_room(room_id, session)) {
				// convert data object to string as no manipulation is need.
				boolean sent_data = session_manager.send_data(room_id, session, to_user, data_obj.toString());
				if (sent_data == false) {
					JSONObject res_obj = session_manager.create_response("data not sent");

					session.getBasicRemote().sendText(res_obj.toString());
					System.out.println(room_id + " data not sent !");
				}
			} else {
				JSONObject res_obj = session_manager.create_response("user not in room");

				session.getBasicRemote().sendText(res_obj.toString());
				System.out.println(room_id + " user not in room !");
			}
		}

		if (action.equals("Leave Room")) {
			if (session_manager.is_in_room(room_id, session)) {
				String user = (String) session.getUserProperties().get("username");
				if (session_manager.remove_user_from_room(room_id, session)) {
					System.out.println(user + " Left !");
				}
			} else {
				JSONObject res_obj = session_manager.create_response("user not in room");

				session.getBasicRemote().sendText(res_obj.toString());
				System.out.println(room_id + " user not in room !");
			}
		}

	}

	@OnClose
	public void whenClosing(Session session) throws IOException {
		String room = (String) session.getUserProperties().get("room_id");
		String user = (String) session.getUserProperties().get("username");
		if (session_manager.remove_user_from_room(room, session)) {
			System.out.println(user + " Left !");
		}
		System.out.println("Close!");
	}
}

class SessionManager {
	private static final HashMap<String, ArrayList<Session>> rooms = new HashMap<String, ArrayList<Session>>();;

	public boolean room_exists(String room_id) {

		for (Map.Entry<String, ArrayList<Session>> set : rooms.entrySet()) {
			if (set.getKey().equals(room_id)) {
				return true;
			}
		}
		return false;
	}

	public boolean is_in_room(String room_id, Session sess) {

		for (Session session : rooms.get(room_id)) {
			if (session.equals(sess)) {
				return true;
			}
		}
		return false;
	}

	public void create_room(String username, String room_id, Session sess) throws IOException {
		rooms.put(room_id, new ArrayList<Session>());
		System.out.println(room_id + " Created !");
		join_room(username, room_id, sess);
	}

	public void join_room(String username, String room_id, Session sess) {
		ArrayList<Session> list = rooms.get(room_id);

		sess.getUserProperties().put("username", username);
		sess.getUserProperties().put("room_id", room_id);
		list.add(sess);
		System.out.println(sess.getId() + " Joined " + room_id);
	}

	public boolean remove_user_from_room(String room_id, Session sess) throws IOException {
		ArrayList<Session> participants = rooms.get(room_id);
		String user = (String) sess.getUserProperties().get("username");

		//this if is here to handle unexpected closing
		if (participants == null) {
			System.out.println("Room already closed");
		} else {
			if (participants.remove(sess)) {
				if (participants.isEmpty()) {
					rooms.remove(room_id);
				} else {
					for (Session session : participants) {
						JSONObject res_obj = create_response("left room", user);
						session.getBasicRemote().sendText(res_obj.toString());
					}
				}
				return true;
			}
		}
		return false;
	}

	public ArrayList<String> get_participants_username(String room_id) {
		ArrayList<String> usernames = new ArrayList<String>();

		for (Session session : rooms.get(room_id)) {
			String username = (String) session.getUserProperties().get("username");
			usernames.add(username);
		}

		return usernames;
	}

	public boolean send_data(String room_id, Session sess, String to_user, String data_obj_str) throws IOException {
		JSONObject res_obj = create_response("connection data", data_obj_str);

		for (Session session : rooms.get(room_id)) {
			String username = (String) session.getUserProperties().get("username");
			if (username.equals(to_user)) {
				session.getBasicRemote().sendText(res_obj.toString());
				return true;
			}
		}
		return false;
	}

	public JSONObject create_response(String response) {
		JSONObject res_obj = new JSONObject();
		res_obj.put("response", response);
		res_obj.put("data", "{}");
		return res_obj;
	}

	public JSONObject create_response(String response, String data_obj_str) {
		JSONObject res_obj = new JSONObject();
		res_obj.put("response", response);
		res_obj.put("data", data_obj_str);
		return res_obj;
	}

}