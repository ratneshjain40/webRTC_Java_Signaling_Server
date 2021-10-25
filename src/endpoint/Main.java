package endpoint;

import java.io.IOException;
import java.util.ArrayList;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Signaling server to WebRTC video conferencing.
 */
@ServerEndpoint("/signal")
public class Main {

	static final SessionManager session_manager = new SessionManager();

	@OnOpen
	public void whenOpening(Session session) throws EncodeException {
		session.setMaxIdleTimeout(30000);
		System.out.println("Open! " + session.getId());
	}

	@OnMessage
	public void process(String data, Session session) throws IOException, JSONException {
		System.out.println("Got signal - " + data);
		JSONObject client_obj = new JSONObject(data);
		
		// here action and room_id are the only mandatory fields by the client
		String action = client_obj.getString("action");
		String room_id = client_obj.getString("room_id");
		
		if (action.equals("Active")) {
			System.out.println(session.getId() + " is active !");
		}
		
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
		
		// send data enables direct communication between clients in same room
		if (action.equals("Send Data")) {

			String to_user = client_obj.getString("to_user");
			JSONObject data_obj = client_obj.getJSONObject("data");

			if (session_manager.is_in_room(room_id, session)) {
				// converting data object to string as no manipulation is need.
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
			if (session_manager.room_exists(room_id)) {

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
	
	@OnError
	public void error(Session session, Throwable t) {
		System.out.println("Error! in session : " + session.getId() + "Message : " + t.getMessage());
	}
}