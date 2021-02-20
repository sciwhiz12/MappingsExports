public enum Side {
    CLIENT,
    SERVER,
    BOTH;

    public static Side from(int value) {
        return values()[value];
    }

    public int toNumber() {
        return this.ordinal();
    }
}
