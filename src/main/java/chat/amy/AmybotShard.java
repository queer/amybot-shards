package chat.amy;

import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;

/**
 * @author amy
 * @since 9/2/17.
 */
public final class AmybotShard {
    @Getter
    private JDA jda;
    
    private AmybotShard() {
    }
    
    public static void main(final String[] args) {
        new AmybotShard().start();
    }
    
    private void start() {
    
    }
    
    private void startBot(final int shardId, final int shardCount) {
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .useSharding(shardId, shardCount)
                    .setToken(System.getenv("BOT_TOKEN"))
                    .buildAsync();
        } catch(final LoginException | RateLimitedException e) {
            e.printStackTrace();
        }
    }
}
