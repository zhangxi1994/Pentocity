package pentos.g2;

// Union-find data structure for
// connected-component labeling algorithm
class UF {
    private UF _parent;
    private int _label;

    public UF(int label) {
        _parent = this;
        _label = label;
    }

    public int getLabel() {
        return _label;
    }

    public UF findRoot() {
        if (isRoot()) {
            return this;
        } else {
            return _parent.findRoot();
        }
    }

    public static void union(UF x, UF y) {
        UF xRoot = x.findRoot();
        UF yRoot = y.findRoot();

        xRoot._parent = yRoot;
    }

    public boolean isRoot() {
        return _parent == this;
    }
}