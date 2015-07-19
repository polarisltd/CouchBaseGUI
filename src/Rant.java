
import java.util.UUID;

public class Rant {
        private final UUID id;
        private final String username;
        private final String ranttext;
        private final String type;
        public Rant(UUID id, String username, String ranttext, String type) {
                this.id = id;
                this.username = username;
                this.ranttext = ranttext;
                this.type = type;
}
        public UUID getId() {
                return id;
}
        public String getUsername() {
                return username;
}
        public String getRanttext() {
                return ranttext;
} }
