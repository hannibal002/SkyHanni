package at.hannibal2.skyhanni.config.features.garden.visitor;

public enum VisitorBlockBehaviour {
    DONT("Don't"), ALWAYS("Always"), ONLY_ON_BINGO("Only on Bingo");

    final String str;

    VisitorBlockBehaviour(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
