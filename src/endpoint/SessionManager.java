package endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import org.json.JSONObject;
public class SessionManager {
	private static final HashMap<String, ArrayList<Session>> rooms = new HashMap<String, ArrayList<Session>>();

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

	public void create_room(String username, String room_id, Session sess) {
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

		// this 'if' is here to handle unexpected closing
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