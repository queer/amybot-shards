package chat.amy.cache.presence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {
    private String name;
    private GameType type;
    private String url;
    
    public enum GameType {
        GAME(0),
        STREAMING(1);
        
        private final int id;
    
        GameType(final int id) {
            this.id = id;
        }
        
        @JsonValue
        public int toValue() {
            return id;
        }
    }
}
