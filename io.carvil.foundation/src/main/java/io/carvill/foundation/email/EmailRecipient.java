package io.carvill.foundation.email;

/**
 * @author Carlos Carpio, carlos.carpio07@gmail.com
 */
public abstract class EmailRecipient implements Recipient {

    public String getName() {
        return this.getEmail();
    }

}
