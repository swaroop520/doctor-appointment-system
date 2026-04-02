import org.springframework.security.crypto.bcrypt.BCrypt;

public class BCryptGen {
    public static void main(String[] args) {
        String password = "password123";
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt(10));
        System.out.println("HASH:" + hashed);
    }
}
