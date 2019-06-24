package lan.groland.eve.adapter.port.ws;

/**
 * Thrown when provider format is invalid
 * @author alexandre
 *
 */
public class WrongFormatException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public WrongFormatException() {
  }

  public WrongFormatException(String message) {
    super(message);
  }

  public WrongFormatException(Throwable cause) {
    super(cause);
  }

  public WrongFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public WrongFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
