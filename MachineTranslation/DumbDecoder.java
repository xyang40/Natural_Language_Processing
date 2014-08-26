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

public class DumbDecoder {

	private HashMap<String, HashMap<String, NT>> model;
	private HashMap<String, HashSet<String>> rev;

	public DumbDecoder() {
		this.model = new HashMap<String, HashMap<String, NT>>();
		this.rev = new HashMap<String, HashSet<String>>();
	}

	public static void main(String[] args) throws IOException {
		DumbDecoder newinstance = new DumbDecoder();
		/*
		String filename1 = "/home/xi/mt/data/english-senate-0.txt";
		String filename2 = "/home/xi/mt/data/french-senate-0.txt";
		String filename3 = "/home/xi/mt/data/french-senate-2.txt";
		String writefile = "/home/xi/mt/data/translation.txt";
		String comparefile = "/home/xi/mt/data/english-senate-2.txt";
		*/
		String filename1 = args[1];
		String filename2 = args[0];
		String filename3 = args[2];
		
		String writefile = "translation.txt";
		String comparefile = "english-senate-2.txt";
		
		File une = new File(filename1);
		File deux = new File(filename2);
		File trois = new File(filename3);
		File quatre = new File(writefile);
		File cinq = new File(comparefile);

		int iterations = 10;
		newinstance.getModel(une, deux, iterations);
		// System.out.println("optimizing");
		// newinstance.optimizeTau(0.5);
		System.out.println("decode");
		newinstance.decode(trois, quatre, cinq);
	}

	public void getModel(File une, File deux, int iterations) throws IOException {
		Scanner scan_une_ini = new Scanner(new BufferedReader(new FileReader(une)));
		Scanner scan_deux_ini = new Scanner(new BufferedReader(new FileReader(deux)));
		ArrayList<String[]> list_une = new ArrayList<String[]>();
		ArrayList<String[]> list_deux = new ArrayList<String[]>();
		int row_count = 0;

		while (scan_une_ini.hasNextLine() && scan_deux_ini.hasNextLine()) {
			String line_une = scan_une_ini.nextLine().trim();
			String line_deux = scan_deux_ini.nextLine().trim();
			String[] temp_une = line_une.split(" ");
			String[] temp_deux = line_deux.split(" ");
			list_une.add(row_count, temp_une);
			list_deux.add(row_count, temp_deux);
			for (int i = 0; i <= temp_une.length - 1; i++) {
				if (!this.model.containsKey(temp_une[i])) {
					this.model.put(temp_une[i], new HashMap<String, NT>());
				}
				for (int j = 0; j <= temp_deux.length - 1; j++) {
					this.model.get(temp_une[i]).put(temp_deux[j], new NT(0.0, 1.0));
				}
			}
			row_count++;
		}
		scan_une_ini.close();
		scan_deux_ini.close();
		Iterator<Map.Entry<String, HashMap<String, NT>>> iterator_m = this.model.entrySet().iterator();
		while (iterator_m.hasNext()) {
			Map.Entry<String, HashMap<String, NT>> entry = iterator_m.next();
			String left = entry.getKey();
			HashMap<String, NT> hm = entry.getValue();
			Iterator<String> ite = hm.keySet().iterator();
			while (ite.hasNext()) {
				String right = ite.next();
				if (!this.rev.containsKey(right)) {
					this.rev.put(right, new HashSet<String>());
				}
				this.rev.get(right).add(left);
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
						matrix_n[i][j] = this.model.get(sen_une[i]).get(sen_deux[j]).getN();
						matrix_tau[i][j] = this.model.get(sen_une[i]).get(sen_deux[j]).getT();
						sum_tau += matrix_tau[i][j];
					}

					for (int j = 0; j <= sen_deux.length - 1; j++) {
						double new_n = matrix_n[i][j] + matrix_tau[i][j] / sum_tau;
						this.model.get(sen_une[i]).put(sen_deux[j], new NT(new_n, matrix_tau[i][j]));
					}
				}
			}

			System.out.println("M");
			Iterator<Map.Entry<String, HashSet<String>>> iterator = this.rev.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, HashSet<String>> entry = iterator.next();
				String left = entry.getKey();
				HashSet<String> hs = entry.getValue();
				double sum_n = 0.0;
				for (String right : hs) {
					sum_n += this.model.get(right).get(left).getN();
				}
				for (String right : hs) {
					this.model.get(right).put(left, new NT(0.0, this.model.get(right).get(left).getN() / sum_n));
				}
			}
		}
	}

	public void optimizeTau(double epsilon) {

	}

	public void decode(File trois, File quatre, File cinq) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(quatre)), false);
		Scanner scanner1 = new Scanner(new BufferedReader(new FileReader(trois)));
		Scanner scanner2 = new Scanner(new BufferedReader(new FileReader(cinq)));
		ArrayList<String> to_translate = new ArrayList<String>();
		ArrayList<String> to_compare = new ArrayList<String>();

		int row_count = 0;
		while (scanner1.hasNextLine() && scanner2.hasNextLine()) {
			String line1 = scanner1.nextLine().trim();
			String line2 = scanner2.nextLine().trim();
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
					double max_prob = -99;
					String translation = word;
					if (this.rev.containsKey(word)) {
						HashSet<String> hs = this.rev.get(word);
						for (String candidate : hs) {
							double cur_tau = this.model.get(candidate).get(word).getT();
							if (cur_tau > max_prob) {
								max_prob = cur_tau;
								translation = candidate;
							}
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
                        else{
			pw.println("");
			pw.flush();}
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
