package me.project.http;

import me.project.exception.ParseException;
import me.project.utils.HttpUtils;

public enum RequestPreambleState {

    REQUEST_METHOD {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == ' ') {
                return REQUEST_METHOD_SP;
            } else if(HttpUtils.isTokenCharacter(ch)) {
                return REQUEST_METHOD;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return true;
        }
    },

    REQUEST_METHOD_SP {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == ' ') {
                return REQUEST_METHOD_SP;
            } else if(HttpUtils.isURICharacter(ch)) {
                return REQUEST_PATH;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    REQUEST_PATH {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == ' ') {
                return REQUEST_PATH_SP;
            } else if(HttpUtils.isURICharacter(ch)) {
                return REQUEST_PATH;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return true;
        }
    },

    REQUEST_PATH_SP {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == ' ') {
                return REQUEST_PATH_SP;
            } else if(HttpUtils.isURICharacter(ch)) {
                return REQUEST_PROTOCOL;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    REQUEST_PROTOCOL {
        @Override
        public RequestPreambleState next(byte ch) {
            if (ch == 'H' || ch == 'T' || ch == 'P' || ch == '/' || ch == '1' || ch == '.') {
                return REQUEST_PROTOCOL;
            } else if (ch == '\r') {
                return REQUEST_CR;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return true;
        }
    },

    REQUEST_CR {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == '\n') {
                return REQUEST_LF;
            }
            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    REQUEST_LF {
        @Override
        public RequestPreambleState next(byte ch) {
            if (ch == '\r') {
                return PREAMBLE_CR;
            } else if (HttpUtils.isTokenCharacter(ch)) {
                return HEADER_NAME;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    HEADER_NAME {
        @Override
        public RequestPreambleState next(byte ch) {
            if(HttpUtils.isTokenCharacter(ch)) {
                return HEADER_NAME;
            } else if(ch == ':') {
                return HEADER_COLON;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return true;
        }
    },

    HEADER_COLON {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == ' ') {
                return HEADER_COLON;
            } else if(ch == '\r') {
                return HEADER_CR;
            }

            return HEADER_VALUE;
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    HEADER_VALUE {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == '\r') {
                return HEADER_CR;
            } else if(HttpUtils.isValueCharacter(ch)) {
                return HEADER_VALUE;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return true;
        }
    },

    HEADER_CR {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == '\n') {
                return HEADER_LF;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    HEADER_LF {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == '\r') {
                return PREAMBLE_CR;
            } else if(HttpUtils.isTokenCharacter(ch)) {
                return HEADER_NAME;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    PREAMBLE_CR {
        @Override
        public RequestPreambleState next(byte ch) {
            if(ch == '\n') {
                return COMPLETE;
            }

            throw makeParseException((char) ch);
        }

        @Override
        public boolean store() {
            return false;
        }
    },

    COMPLETE {
        @Override
        public RequestPreambleState next(byte ch) {
            return null;
        }

        @Override
        public boolean store() {
            return false;
        }
    };

    public abstract RequestPreambleState next(byte ch);

    public abstract boolean store();

    ParseException makeParseException(char ch) {
        return new ParseException("Invalid character [" + ch + "] in state [" + this + "]");
    }
}
