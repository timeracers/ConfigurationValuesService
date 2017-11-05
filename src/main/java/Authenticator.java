import java.util.Optional;
import java.util.function.Supplier;

public class Authenticator {
    private final Optional<String> password;

    public Authenticator(String password) {
        this.password = Optional.of(password);
    }

    public ResponseWrapper authenticated(Optional<String> password, Supplier<ResponseWrapper> function) {
        if(this.password.equals(password))
            return function.get();
        else if(!password.isPresent())
            return new ResponseWrapper(401, "UnauthorizedException" ,
                "Authorization is required");
        return new ResponseWrapper(401, "UnauthorizedException" , "Password is incorrect");
    }
}
