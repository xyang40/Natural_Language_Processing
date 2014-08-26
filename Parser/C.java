public class C {

	private String label;
	private C left;
	private C right;
	private double p;
	private int ary;// 1 or 2

	public C(String label, C left, C right, double p, int a) {
		this.setLabel(label);
		this.setLeft(left);
		this.setRight(right);
		this.setP(p);
		this.setAry(a);
	}

	public boolean equal(Object o) {
		C other = (C) o;
		if (other.getAry() != this.getAry()) {
			return false;
		} else {
			if (this.getAry() == 2) {
				return this.label.equals(other.getLabel()) && this.left.getLabel().equals(other.getLeft().getLabel()) && this.right.getLabel().equals(other.getRight().getLabel());
			} else {
				return this.label.equals(other.getLabel()) && this.left.getLabel().equals(other.getLeft().getLabel());
			}
		}
	}

	// public boolean equal(Object o){
	// c = (C)o;
	// return this.label.equals(c.getLabel()) && this.left.label.equals(c.getle)

	// }

	public double getP() {
		return p;
	}

	public void setP(double p) {
		this.p = p;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public C getLeft() {
		return left;
	}

	public void setLeft(C left) {
		this.left = left;
	}

	public C getRight() {
		return right;
	}

	public void setRight(C right) {
		this.right = right;
	}

	public int getAry() {
		return ary;
	}

	public void setAry(int ary) {
		this.ary = ary;
	}

}
