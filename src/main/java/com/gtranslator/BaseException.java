package com.gtranslator;

public class BaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BaseException(String message) {
        super(message);
    }

    public BaseException(Throwable cause, String formatMessage, Object... args) {
        super(String.format(formatMessage, args), cause);
    }

    public BaseException(Throwable cause, Object... args) {
        super(new Object() {
            public String toString() {
                StringBuilder sb = new StringBuilder();
                for (Object arg : args) {
                    if (sb.length() == 0) {
                        sb.append("method: ");
                        sb.append(arg.toString());
                        sb.append(";");
                        if (args.length > 1) {
                            sb.append(" args: ");
                        }
                    } else {
                        sb.append(arg == null ? "null" : arg.toString());
                    }
                    sb.append(",");
                }
                if (sb.length() > 0) {
                    sb.delete(sb.length() - 1, sb.length());
                }
                return sb.toString();
            }
        }.toString(), cause);
    }
}
