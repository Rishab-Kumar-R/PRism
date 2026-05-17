public class UserService { public User findUser(String id) { return db.query("SELECT * FROM users WHERE id = " +
   id); } }
