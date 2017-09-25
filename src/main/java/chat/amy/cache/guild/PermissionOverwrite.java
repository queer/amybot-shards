package chat.amy.cache.guild;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class PermissionOverwrite {
    private String id;
    private OverwriteType type;
    private int allow;
    private int deny;
    
    public enum OverwriteType {
        ROLE("role"),
        MEMBER("member");
        
        private final String type;
    
        OverwriteType(final String type) {
            this.type = type;
        }
        
        @JsonValue
        public String toValue() {
            return type;
        }
    }
}
