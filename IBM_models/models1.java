import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;

public class models1 {

	private HashMap<String, Integer> unigramCnt;
	private HashMap<String, Integer> bigramCnt;

	private HashMap<String, Double> unigrammodel;
	private HashMap<String, Double> unigrammodel_new;
	private HashMap<String, Double> bigrammodel;
	private HashMap<String, Double> bigrammodel_new;

	private int tokentotal;
	private int typetotal;

	private double alpha;
	private double beta;

	public models1() {
		this.unigramCnt = new HashMap<String, Integer>();
		this.bigramCnt = new HashMap<String, Integer>();

		this.unigrammodel = new HashMap<String, Double>();
		this.unigrammodel_new = new HashMap<String, Double>();
		this.bigrammodel = new HashMap<String, Double>();
		this.bigrammodel_new = new HashMap<String, Double>();

		this.tokentotal = 0;
		this.typetotal = 0;
		this.alpha = 0;
		this.beta = 0;
	}

	public static void main(String[] args) throws FileNotFoundException {
		String trainfile = args[0];
		String devfile = args[1];
		String testfile = args[2];
		String goodbad = args[3];

		models1 model = new models1();

		// P1
		model.P1(trainfile, testfile);

		// P2
		model.P2(devfile, testfile);

		// P3
		model.P3(goodbad);

		// P4
		//model.P4(trainfile, testfile, goodbad);

		// P5
		//model.P5(devfile, testfile, goodbad);

		// P6
		//model.P6(goodbad);
	}

	public void P1(String trainfile, String testfile)
			throws FileNotFoundException {
		getUnigramModel(trainfile);
		getLikelihood(testfile, this.unigrammodel, 1);
	}

	private void getUnigramModel(String trainfile) throws FileNotFoundException {
		File file = new File(trainfile);
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

		int sum = 0;

		while (scanner.hasNextLine()) {
			String line = "#START# " + scanner.nextLine().trim() + " #END#";
			String[] temp = line.split(" ");
			sum += temp.length;

			for (String string : temp) {
				Integer count = unigramCnt.get(string);
				unigramCnt.put(string, (count == null) ? 1 : count + 1);
			}
		}
		unigramCnt.put("#UNBEKANNT#", 0);// put in the unknown word
		this.typetotal = unigramCnt.size();// plus unknown
		this.tokentotal = sum;// total number of tokens in the
								// corpusFileNotFoundException

		for (Map.Entry<String, Integer> entry : unigramCnt.entrySet()) {
			String unigram = entry.getKey();
			Integer count = entry.getValue();

			this.unigrammodel.put(unigram, (count + 1.0)
					/ (this.tokentotal + this.typetotal));

		}
		scanner.close();
	}

	private void getLikelihood(String testfile, HashMap<String, Double> model,
			int radix) throws FileNotFoundException {
		LinkedList<Double> list = new LinkedList<Double>();
		File file = new File(testfile);
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

		while (scanner.hasNextLine()) {
			double sum = 0;
			String line = "#START# " + scanner.nextLine().trim() + " #END#";
			String[] temp = line.split(" ");

			if (radix == 1) {
				for (String string : temp) {
					if (model.containsKey(string)) {
						sum += Math.log(model.get(string));
					} else {
						sum += Math.log(model.get("#UNBEKANNT#"));
					}
				}
			}
			if (radix == 2) {
				for (int i = 0; i <= temp.length - 2; i++) {
					String string = temp[i] + " " + temp[i + 1];
					if (model.containsKey(string)) {
						sum += Math.log(model.get(string));
					} else {
						double numerator = 0;
						double denominator = 0;
						if (this.unigrammodel.containsKey(temp[i + 1])) {
							numerator = this.unigrammodel.get(temp[i + 1]);
						} else {
							numerator = this.unigrammodel.get("#UNBEKANNT#");
						}
						if (this.unigramCnt.containsKey(temp[i])) {
							denominator = this.unigramCnt.get(temp[i]);
						} else {
							denominator = 0;
						}
						sum += Math.log(numerator / (1 + denominator));
					}
				}
			}
			list.add(sum);
		}
		scanner.close();

		double likelihood = 0;
		for (double d : list) {
			likelihood += d;
		}
		System.out.println("The Total Likelihood is: " + likelihood);
	}

	public void P2(String devfile, String testfile)
			throws FileNotFoundException {
		getNewUnigramModel(devfile);
		getLikelihood(testfile, this.unigrammodel_new, 1);
	}

	private void getNewUnigramModel(String devfile)
			throws FileNotFoundException {

		this.alpha = optimizeParam(devfile, 0, 400, 800, 0.01, 1);
		//System.out.println("alpha " + this.alpha);

		for (Map.Entry<String, Integer> entry : unigramCnt.entrySet()) {
			this.unigrammodel_new.put(entry.getKey(),
					(entry.getValue() + this.alpha)
							/ (this.tokentotal + this.alpha * this.typetotal));
		}
	}

	private double optimizeParam(String devfile, double a, double b, double c,
			double tau, int radix) throws FileNotFoundException {
		double phi = (1 + Math.sqrt(5)) / 2;
		double resphi = 2 - phi;
		double x;
		if (c - b > b - a)
			x = b + resphi * (c - b);
		else
			x = b - resphi * (b - a);
		if (Math.abs(c - a) < tau * (Math.abs(b) + Math.abs(x)))
			return (c + a) / 2;
		assert (f(devfile, x, radix) != f(devfile, b, radix));
		if (f(devfile, x, radix) < f(devfile, b, radix)) {
			if (c - b > b - a)
				return optimizeParam(devfile, b, x, c, tau, radix);
			else
				return optimizeParam(devfile, a, x, b, tau, radix);
		} else {
			if (c - b > b - a)
				return optimizeParam(devfile, a, b, x, tau, radix);
			else
				return optimizeParam(devfile, x, b, c, tau, radix);
		}
	}

	private double f(String devfile, double x, int radix)
			throws FileNotFoundException {
		LinkedList<Double> list = new LinkedList<Double>();
		File file = new File(devfile);
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

		while (scanner.hasNextLine()) {
			double sum = 0;
			String line = "#START# " + scanner.nextLine().trim() + " #END#";
			String[] temp = line.split(" ");
			if (radix == 1) {
				for (String string : temp) {
					if (this.unigrammodel.containsKey(string)) {
						sum += Math.log((unigramCnt.get(string) + x)
								/ (this.tokentotal + x * this.typetotal));
					} else {
						sum += Math.log(x
								/ (this.tokentotal + x * this.typetotal));
					}
				}
			}
			if (radix == 2) {
				for (int i = 0; i <= temp.length - 2; i++) {
					String string = temp[i] + " " + temp[i + 1];
					if (bigrammodel.containsKey(string)) {
						sum += Math.log((bigramCnt.get(string) + x
								* unigrammodel_new.get(temp[i + 1]))
								/ (unigramCnt.get(temp[i]) + x));
					} else {
						double numerator = 0;
						double denominator = 0;
						if (this.unigrammodel_new.containsKey(temp[i + 1])) {
							numerator = x
									* this.unigrammodel_new.get(temp[i + 1]);
						} else {
							numerator = x
									* this.unigrammodel_new.get("#UNBEKANNT#");
						}
						if (this.unigramCnt.containsKey(temp[i])) {
							denominator = this.unigramCnt.get(temp[i]);
						} else {
							denominator = 0;
						}
						sum += Math.log(numerator / (x + denominator));
					}
				}
			}
			list.add(sum);
		}
		scanner.close();

		double likelihood = 0;
		;
		for (double d : list) {
			likelihood += d;
		}
		return -likelihood;
	}

	public void P3(String testfile) throws FileNotFoundException {
		LinkedList<Double> list = new LinkedList<Double>();
		File file = new File(testfile);
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

		while (scanner.hasNextLine()) {
			double sum = 0;
			String line = "#START# " + scanner.nextLine().trim() + " #END#";
			String[] temp = line.split(" ");

			for (String string : temp) {
				if (this.unigrammodel_new.containsKey(string)) {
					sum += Math.log(unigrammodel_new.get(string));
				} else {
					sum += Math.log(unigrammodel_new.get("#UNBEKANNT#"));
				}
			}
			list.add(sum);
		}
		scanner.close();

		int errcnt = 0;
		for (int i = 0; i <= list.size() - 3; i = i + 2) {
			if (list.get(i) <= list.get(i + 1)) {
				errcnt++;
			}
		}
		System.out.println("Accuracy: " + (1 - ((errcnt + 0.0) / list.size())));
		System.out.println("alpha " + this.alpha);
	}

	public void P4(String trainfile, String testfile1, String testfile2)
			throws FileNotFoundException {
		getBigramModel(trainfile);
		System.out.println("Evaluating on english-senate-2: ");
		getLikelihood(testfile1, this.bigrammodel, 2);
		System.out.println("Evaluating on good-bad: ");
		getLikelihood(testfile2, this.bigrammodel, 2);

	}

	private void getBigramModel(String trainfile) throws FileNotFoundException {
		File file = new File(trainfile);
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

		while (scanner.hasNextLine()) {
			String line = "#START# " + scanner.nextLine().trim() + " #END#";
			String[] temp = line.split(" ");
			for (int i = 0; i < temp.length - 2; i++) {
				String string = temp[i] + " " + temp[i + 1];
				Integer count = bigramCnt.get(string);
				bigramCnt.put(string, (count == null) ? 1 : count + 1);
			}
		}

		for (Map.Entry<String, Integer> entry : bigramCnt.entrySet()) {
			String bigram = entry.getKey();
			String fore = bigram.split(" ")[0];
			String hind = bigram.split(" ")[1];
			Integer bigramcount = entry.getValue();
			this.bigrammodel.put(bigram,
					(bigramcount + this.unigrammodel.get(hind))
							/ (this.unigramCnt.get(fore) + 1));
		}

	}

	public void P5(String devfile, String testfile1, String testfile2)
			throws FileNotFoundException {
		getNewBigramModel(devfile);
		System.out.println("Evaluating on english-senate-2: ");
		getLikelihood(testfile1, this.bigrammodel_new, 2);
		System.out.println("Evaluating on good-bad: ");
		getLikelihood(testfile2, this.bigrammodel_new, 2);
	}

	private void getNewBigramModel(String devfile) throws FileNotFoundException {

		this.beta = optimizeParam(devfile, 0, 400, 800, 0.01, 2);
		System.out.println("beta " + this.beta);

		for (Map.Entry<String, Integer> entry : this.bigramCnt.entrySet()) {
			String bigram = entry.getKey();
			String fore = bigram.split(" ")[0];
			String hind = bigram.split(" ")[1];
			Integer bigramcount = entry.getValue();
			this.bigrammodel_new.put(bigram, (bigramcount + this.beta
					* unigrammodel_new.get(hind))
					/ (unigramCnt.get(fore) + this.beta));
		}
	}

	public void P6(String testfile) throws FileNotFoundException {
		LinkedList<Double> list = new LinkedList<Double>();
		File file = new File(testfile);
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

		while (scanner.hasNextLine()) {
			double sum = 0;
			String line = "#START# " + scanner.nextLine().trim() + " #END#";
			String[] temp = line.split(" ");

			for (int i = 0; i <= temp.length - 2; i++) {
				String string = temp[i] + " " + temp[i + 1];
				if (bigrammodel_new.containsKey(string)) {
					sum += Math.log(bigrammodel_new.get(string));
				} else {
					double numerator = 0;
					double denominator = 0;
					if (this.unigrammodel_new.containsKey(temp[i + 1])) {
						numerator = this.unigrammodel_new.get(temp[i + 1]);
					} else {
						numerator = this.unigrammodel_new.get("#UNBEKANNT#");
					}
					if (this.unigramCnt.containsKey(temp[i])) {
						denominator = this.unigramCnt.get(temp[i]);
					} else {
						denominator = 0;
					}
					sum += Math.log(numerator * this.beta
							/ (this.beta + denominator));
				}
			}
			list.add(sum);
		}
		scanner.close();

		int errcnt = 0;
		for (int i = 0; i <= list.size() - 3; i = i + 2) {
			if (list.get(i) <= list.get(i + 1)) {
				errcnt++;
			}
		}
		System.out.println("Accuracy: " + (1 - ((errcnt + 0.0) / list.size())));
	}

}
