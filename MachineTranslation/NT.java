
public class NT {
	
	private double N;
	private double tau;
	
	public NT(double N,double tau){
		this.N=N;
		this.tau=tau;
	}

	public double getN(){
		return this.N;
	}
	public double getT(){
		return this.tau;
	}
	
	public String toString(){
		return this.N +":"+this.tau;
		
	}
}
