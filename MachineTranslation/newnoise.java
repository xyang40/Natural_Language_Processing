import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class newnoise {

	private HashMap<String, HashMap<String, NT>> transmodel;
	private HashMap<String, HashSet<String>> rev_trans;

	// private HashMap<String, Integer> bigramCnt;
	// private HashMap<String, Double> bigrammodel;
	private HashMap<String, HashMap<String, Integer>> bigramCnt;
	private HashMap<String, HashMap<String, Double>> bigrammodel;
	private HashMap<String, Integer> unigramCnt;
	private HashMap<String, Double> unigrammodel;
	private HashMap<String, HashSet<String>> rev_lang;

	public newnoise() {
		this.transmodel = new HashMap<String, HashMap<String, NT>>();
		this.rev_trans = new HashMap<String, HashSet<String>>();
		// this.bigramCnt = new HashMap<String, Integer>();
		// this.bigrammodel = new HashMap<String, Double>();
		this.bigramCnt = new HashMap<String, HashMap<String, Integer>>();
		this.bigrammodel = new HashMap<String, HashMap<String, Double>>();
		this.unigramCnt = new HashMap<String, Integer>();
		this.unigrammodel = new HashMap<String, Double>();
		this.rev_lang = new HashMap<String, HashSet<String>>();// for bigram
																// model
	}

	public static void main(String[] args) throws IOException {
		newnoise newinstance = new newnoise();
                
/*
		String filename1 = "/home/xxyang/mt/data/french-senate-0.txt";
		String filename2 = "/home/xxyang/mt/data/english-senate-0.txt";
		String filename3 = "/home/xxyang/mt/data/french-senate-2.txt";
		String writefile = "/home/xxyang/mt/data/translation_nc.txt";
*/
                String filename1 = args[0];
		String filename2 = args[1];
		String filename3 = args[2];
		
		String writefile = "translation_noisychannel.txt";
		String comparefile = "english-senate-2.txt";

		File une = new File(filename1);
		File deux = new File(filename2);
		File trois = new File(filename3);
		File quatre = new File(writefile);
		File cinq = new File(comparefile);

		int iterations = 10;
		newinstance.getTransModel(une, deux, iterations);
		newinstance.getLangModels(deux);
		// System.out.println("optimizing");
		// newinstance.optimizeTau(0.5);
		System.out.println("decode");
		newinstance.decode(trois, quatre, cinq);
	}

	public void getTransModel(File une, File deux, int iterations) throws IOException {
		Scanner scan_une_ini = new Scanner(new BufferedReader(new FileReader(une)));
		Scanner scan_deux_ini = new Scanner(new BufferedReader(new FileReader(deux)));
		ArrayList<String[]> list_une = new ArrayList<String[]>();
		ArrayList<String[]> list_deux = new ArrayList<String[]>();
		int row_count = 0;

		while (scan_une_ini.hasNextLine() && scan_deux_ini.hasNextLine()) {
			String line_une = "#START# " + scan_une_ini.nextLine().trim();
			String line_deux = "#START# " + scan_deux_ini.nextLine().trim();
			String[] temp_une = line_une.split(" ");
			String[] temp_deux = line_deux.split(" ");
			list_une.add(row_count, temp_une);
			list_deux.add(row_count, temp_deux);
			for (int i = 0; i <= temp_une.length - 1; i++) {
				if (!this.transmodel.containsKey(temp_une[i])) {
					this.transmodel.put(temp_une[i], new HashMap<String, NT>());
				}
				for (int j = 0; j <= temp_deux.length - 1; j++) {
					this.transmodel.get(temp_une[i]).put(temp_deux[j], new NT(0.0, 1.0));
				}
			}
			row_count++;
		}
		scan_une_ini.close();
		scan_deux_ini.close();
		Iterator<Map.Entry<String, HashMap<String, NT>>> iterator_m = this.transmodel.entrySet().iterator();
		while (iterator_m.hasNext()) {
			Map.Entry<String, HashMap<String, NT>> entry = iterator_m.next();
			String left = entry.getKey();
			HashMap<String, NT> hm = entry.getValue();
			Iterator<String> ite = hm.keySet().iterator();
			while (ite.hasNext()) {
				String right = ite.next();
				if (!this.rev_trans.containsKey(right)) {
					this.rev_trans.put(right, new HashSet<String>());
				}
				this.rev_trans.get(right).add(left);
			}
		}

		for (int iteration = 0; iteration <= iterations - 1; iteration++) {
			System.out.println("iteration " + iteration);
			System.out.println("E");
			for (int row = 0; row <= row_count - 1; row++) {
				String[] sen_une = list_une.get(row);
				String[] sen_deux = list_deux.get(row);
				double[][] matrix_n = new double[sen_une.length][sen_deux.length];
				double[][] matrix_tau = new double[sen_une.length][sen_deux.length];

				for (int i = 0; i <= sen_une.length - 1; i++) {
					double sum_tau = 0.0;
					for (int j = 0; j <= sen_deux.length - 1; j++) {
						matrix_n[i][j] = this.transmodel.get(sen_une[i]).get(sen_deux[j]).getN();
						matrix_tau[i][j] = this.transmodel.get(sen_une[i]).get(sen_deux[j]).getT();
						sum_tau += matrix_tau[i][j];
					}

					for (int j = 0; j <= sen_deux.length - 1; j++) {
						double new_n = matrix_n[i][j] + matrix_tau[i][j] / sum_tau;
						this.transmodel.get(sen_une[i]).put(sen_deux[j], new NT(new_n, matrix_tau[i][j]));
					}
				}
			}

			System.out.println("M");
			Iterator<Map.Entry<String, HashSet<String>>> iterator = this.rev_trans.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, HashSet<String>> entry = iterator.next();
				String left = entry.getKey();
				HashSet<String> hs = entry.getValue();
				double sum_n = 0.0;
				for (String right : hs) {
					sum_n += this.transmodel.get(right).get(left).getN();
				}
				for (String right : hs) {
					this.transmodel.get(right).put(left, new NT(0.0, this.transmodel.get(right).get(left).getN() / sum_n));
				}
			}
		}
	}

	private void getUnigramModel(File file) throws FileNotFoundException {

		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));
		int sum = 0;
		while (scanner.hasNextLine()) {
			String line = "#START# " + scanner.nextLine().trim();
			String[] temp = line.split(" ");
			sum += temp.length;

			for (String string : temp) {
				Integer count = unigramCnt.get(string);
				unigramCnt.put(string, (count == null) ? 1 : count + 1);
			}
		}
		unigramCnt.put("#UNBEKANNT#", 0);
		int typetotal = unigramCnt.size();
		int tokentotal = sum;

		for (Map.Entry<String, Integer> entry : unigramCnt.entrySet()) {
			String unigram = entry.getKey();
			Integer count = entry.getValue();
			this.unigrammodel.put(unigram, (count + 1.5915) / (tokentotal + 1.5915 * typetotal));
		}
		scanner.close();
	}

	private void getBigramModel(File file) throws FileNotFoundException {
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));
		while (scanner.hasNextLine()) {
			String line = "#START# " + scanner.nextLine().trim();
			String[] temp = line.split(" ");
			for (int i = 0; i <= temp.length - 2; i++) {
				if (!this.bigramCnt.containsKey(temp[i])) {
					this.bigramCnt.put(temp[i], new HashMap<String, Integer>());
				}
				if (!this.bigramCnt.get(temp[i]).containsKey(temp[i + 1])) {
					this.bigramCnt.get(temp[i]).put(temp[i + 1], 0);
				}
				Integer count = this.bigramCnt.get(temp[i]).get(temp[i + 1]);
				this.bigramCnt.get(temp[i]).put(temp[i + 1], count + 1);
			}
		}

		for (Map.Entry<String, HashMap<String, Integer>> entry : this.bigramCnt.entrySet()) {
			String fore = entry.getKey();
			HashMap<String, Integer> hindset = entry.getValue();
			if (!this.bigrammodel.containsKey(fore)) {
				this.bigrammodel.put(fore, new HashMap<String, Double>());
			}
			for (Map.Entry<String, Integer> hindentry : hindset.entrySet()) {
				String hind = hindentry.getKey();
				int count = hindentry.getValue();
				this.bigrammodel.get(fore).put(hind, count + 183.97 * this.unigrammodel.get(hind) / (this.unigramCnt.get(fore) + 183.97));
				if (!this.rev_lang.containsKey(hind)) {// initializing rev_lang
					this.rev_lang.put(hind, new HashSet<String>());
				}
				this.rev_lang.get(hind).add(fore);
			}
		}

		scanner.close();
	}

	public void getLangModels(File trainfile) throws IOException {
		getUnigramModel(trainfile);
		getBigramModel(trainfile);
		/*
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File("/home/xi/rev.txt"))), false);
		Iterator<Map.Entry<String, HashSet<String>>> iterator = this.rev_lang.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, HashSet<String>> entry = iterator.next();
			String left = entry.getKey();
			HashSet<String> rightset = entry.getValue();
			for (String right : rightset) {
				pw.println(left + " : " + right);
			}
		}
		pw.close();*/
	}

	public void decode(File trois, File quatre, File cinq) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(quatre)), false);
		Scanner scanner1 = new Scanner(new BufferedReader(new FileReader(trois)));
		Scanner scanner2 = new Scanner(new BufferedReader(new FileReader(cinq)));
		ArrayList<String> to_translate = new ArrayList<String>();
		ArrayList<String> to_compare = new ArrayList<String>();

		int row_count = 0;
		while (scanner1.hasNextLine() && scanner2.hasNextLine()) {
			String line1 =  scanner1.nextLine().trim();
			String line2 =  scanner2.nextLine().trim();
			to_translate.add(row_count, line1);
			to_compare.add(row_count, line2);
			row_count++;
		}
		scanner1.close();
		scanner2.close();

		//int count_getright = 0;
		//int sum_all_to_translate = 0;
		//int sum_all_to_compare = 0;
		for (int i = 0; i <= row_count - 1; i++) {
			String line_to_translate = to_translate.get(i);
			String[] temp = line_to_translate.split(" ");

			if (temp.length <= 10) {
				String line_to_compare = to_compare.get(i);
				//sum_all_to_translate += temp.length;
				//sum_all_to_compare += line_to_compare.split(" ").length;

				StringBuilder sentence = new StringBuilder();
				 for (String word : temp) {
		//		for(int j = 1;j<=temp.length-1;j++){
					///String word = temp[j];
					double max_prob = -99;
					String translation = word;
					if (this.transmodel.containsKey(word)) {
						HashMap<String, NT> candidates = this.transmodel.get(word);
						for (Map.Entry<String, NT> entry : candidates.entrySet()) {
							String candidate = entry.getKey();
                                                        if(!candidate.equals("#START#")) {
							double tau = entry.getValue().getT();

							// get the max value of bigram
							double max_inner_prob = -99;
							HashSet<String> hs = this.rev_lang.get(candidate);
                                                        if(hs==null){System.out.println(candidate);}
							for (String preword : hs) {
								double prob = this.bigrammodel.get(preword).get(candidate);
								if (prob > max_inner_prob) {
									max_inner_prob = prob;
								}
							}
							if (max_inner_prob * tau > max_prob) {
								max_prob = max_inner_prob * tau;
								translation = candidate;
							}}
                                                        else{continue;}
						}
					}
					sentence.append(" ").append(translation);

					//if (line_to_compare.indexOf(translation) != -1) {
					//	count_getright++;
					//}
				}
				pw.println(sentence.toString());
				pw.flush();
			}
                      else{pw.println("");pw.flush();}
		}
		// get precision
		//double precision = (count_getright + 0.0) / sum_all_to_translate;
		//System.out.println("precision: " + precision);
		// get recall
		//double recall = (count_getright + 0.0) / sum_all_to_compare;
		//System.out.println("recall: " + recall);
		// F score
		//System.out.println("F-score: " + 2 * (precision * recall) / (precision + recall));

		pw.close();
	}
}
