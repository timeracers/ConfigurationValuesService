public class ResponseWrapper {
    public final int statusCode;
    public final Object response;
    public final String exceptionType;
    public final String exceptionMessage;

    public ResponseWrapper() {
        this.statusCode = 204;
        this.response = null;
        this.exceptionType = null;
        this.exceptionMessage = null;
    }

    public ResponseWrapper(Object response) {
        this.statusCode = 200;
        this.response = response;
        this.exceptionType = null;
        this.exceptionMessage = null;
    }

    public ResponseWrapper(int statusCode, String exceptionType, String exceptionMessage) {
        this.statusCode = statusCode;
        this.response = null;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
    }
}
