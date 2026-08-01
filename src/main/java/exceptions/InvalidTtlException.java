package exceptions;

public class InvalidTtlException extends  IllegalArgumentException{
    public InvalidTtlException(String message){
        super(message);
    }
}
