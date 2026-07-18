package am.techshop.chat.security;

public record ChatIdentity(Long userId, String guestSessionId, boolean admin) {

    public boolean isGuest() {
        return userId == null;
    }
}
