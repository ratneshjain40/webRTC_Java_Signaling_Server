//package endpoint;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//import javax.websocket.EncodeException;
//import javax.websocket.OnClose;
//import javax.websocket.OnMessage;
//import javax.websocket.OnOpen;
//import javax.websocket.Session;
//import javax.websocket.server.ServerEndpoint;
//
//import org.json.JSONObject;
//
///**
// * Signaling server to WebRTC video conferencing.
// */
//@ServerEndpoint("/signal")
//public class main {
//	
//	static final SessionManager session_manager = new SessionManager();
//
//	@OnOpen
//	public void whenOpening(Session session) throws IOException, EncodeException {
//		System.out.println("Open! "+ session.getId());
//	}
//
//	@OnMessage
//	public void process(String data, Session session) throws IOException {
//		System.out.println("Got signal - " + data);
//		JSONObject data_obj = new JSONObject(data);
//		
//		String action = data_obj.getString("action");
//		String room_id = data_obj.getString("room_id");
//		String type = data_obj.getString("type");
//		String conn = data_obj.getString("data");
//		
//		if(action.equals("Create Room")) {
//			if(session_manager.room_exists(room_id)) {
//				JSONObject res_obj= new JSONObject();
//				res_obj.put("response", "room already exists");
//
//				session.getBasicRemote().sendText(res_obj.toString());
//				System.out.println(room_id + " Exists !");
//			} else {
//				session_manager.create_room(room_id, session);
//				JSONObject res_obj= new JSONObject();
//				res_obj.put("response", "room created");
//				session.getBasicRemote().sendText(res_obj.toString());
//			}
//		}
//		
//		if(action.equals("Join Room")) {
//			if(session_manager.room_exists(room_id)) {
//				if(!session_manager.is_in_room(room_id, session))
//				{
//					session_manager.join_room(room_id, session);
//					JSONObject res_obj= new JSONObject();
//					res_obj.put("response", "room joined");
//					session.getBasicRemote().sendText(res_obj.toString());
//				}
//			} else {
//				JSONObject res_obj= new JSONObject();
//				res_obj.put("response", "room does not exist");
//
//				session.getBasicRemote().sendText(res_obj.toString());
//				System.out.println(room_id + "Does not Exist !");
//			}
//		}
//		
//		if(action.equals("Broadcast")) {
//			if(session_manager.is_in_room(room_id,session)) {
//				session_manager.exchange_data(room_id,session,type,conn);
//			} else {
//				JSONObject res_obj= new JSONObject();
//				res_obj.put("response", "user not in room");
//
//				session.getBasicRemote().sendText(res_obj.toString());
//				System.out.println(room_id + " user not in room !");
//			}
//		}
//		
//	}
//
//	@OnClose
//	public void whenClosing(Session session) {
//		System.out.println("Close!");
//	}
//}
//
//class SessionManager {
//	private static final HashMap<String, ArrayList<Session>> rooms = new HashMap<String, ArrayList<Session>>();;
//
//	
//	public boolean room_exists(String room_id) {
//
//		for (Map.Entry<String, ArrayList<Session>> set : rooms.entrySet()) {
//		    if(set.getKey().equals(room_id)) {
//		    	System.out.println("Room Exists !");
//		    	return true;
//		    }
//		}
//		
//		System.out.println("Room Does Not Exists !");
//		return false;
//	}
//	
//	public boolean is_in_room(String room_id,Session sess) {
//
//		for(Session session: rooms.get(room_id)) {
//			if(session.equals(sess)) {
//				return true;
//			}
//		}
//		System.out.println(sess.getId() + " Not in " + room_id);
//		return false;
//	}
//	
//	public void create_room(String room_id, Session sess) throws IOException {
//		rooms.put(room_id, new ArrayList<Session>());
//		System.out.println(room_id + " Created !");
//		join_room(room_id,sess);
//	}
//	
//	public void join_room(String room_id,Session sess) {
//		ArrayList<Session> list = rooms.get(room_id);
//		list.add(sess);
//		System.out.println(sess.getId() + " Joined " + room_id );
//	}
//	
//	public void exchange_data(String room_id, Session sess,String type,String conn_data) throws IOException {
//		JSONObject res_obj= new JSONObject();
//		res_obj.put("response", "connection data");
//		res_obj.put("type", type);
//		res_obj.put("data", conn_data);
//		
//		for(Session session: rooms.get(room_id)) {
//			if(!session.equals(sess)) {
//				session.getBasicRemote().sendText(res_obj.toString());
//			}
//		}
//	}
//}