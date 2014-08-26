import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

public class finallyfinalparser {

	public static void main(String[] args) throws FileNotFoundException {
		 File rules = new File(args[0]);
		 File input = new File(args[1]);
		 File output = new File(args[2]);
		//File rules = new File("/home/xi/Desktop/NLP/hw4/wsj2-21.blt");
		//File input = new File("/home/xi/Desktop/NLP/hw4/wsj24.txt");
		//File output = new File("/home/xi/Desktop/NLP/hw4/output.txt");
		parse(rules, input, output);
	}

	public static void parse(File rules, File input, File output) throws FileNotFoundException {

		HashMap<String, HashMap<String, HashMap<String, Double>>> binaries = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
		HashMap<String, HashMap<String, Double>> unaries = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> getSumforEachLeft = new HashMap<String, Double>();

		// read in all the rules
		Scanner sc_rules = new Scanner(rules);
		while (sc_rules.hasNextLine()) {
			String rule = sc_rules.nextLine().trim();
			String lhs = rule.split("-->")[0].trim();
			String rhs = rule.split("-->")[1].trim();

			String str_p = lhs.split(" ")[0].trim();
			String str_left = lhs.split(" ")[1].trim();

			if (getSumforEachLeft.containsKey(str_left)) {
				double cur = getSumforEachLeft.get(str_left);
				cur += Double.parseDouble(str_p);
				getSumforEachLeft.put(str_left, cur);
			} else {
				getSumforEachLeft.put(str_left, Double.parseDouble(str_p));
			}

			if (rhs.split(" ").length == 2) {
				String left = rhs.split(" ")[0].trim();
				String right = rhs.split(" ")[1].trim();
		
				HashMap<String, Double> f_p = new HashMap<String, Double>();
				f_p.put(str_left, Double.parseDouble(str_p));
				HashMap<String, HashMap<String, Double>> r_f = new HashMap<String, HashMap<String, Double>>();
				r_f.put(right, f_p);
				if (binaries.containsKey(left)) {
					HashMap<String, HashMap<String, Double>> cur = binaries.get(left);
					if(cur.containsKey(right)){
						HashMap<String,Double> cur1 = cur.get(right);
						if(cur1.containsKey(str_left)){
							System.out.println("IMPOSSIBLE");
						}
						else{
							cur1.put(str_left, Double.parseDouble(str_p));
							cur.put(right, cur1);
						}
					}
					else{
						cur.putAll(r_f);
					}

					binaries.put(left, cur);
				} else {
					binaries.put(left, r_f);
				}
			} else if (rhs.split(" ").length == 1) {
				HashMap<String, Double> f_p = new HashMap<String, Double>();
				f_p.put(str_left, Double.parseDouble(str_p));
				if (unaries.containsKey(rhs)) {
					HashMap<String, Double> cur = unaries.get(rhs);
					cur.putAll(f_p);
					unaries.put(rhs, cur);
				} else {
					unaries.put(rhs, f_p);
				}
			}
		}
		sc_rules.close();
		//System.out.println(" ff NP->NNP NNP " + binaries.get("NNP").get("NNP").get("NP"));

		for (Map.Entry<String, HashMap<String, HashMap<String, Double>>> entry : binaries.entrySet()) {
			String left = entry.getKey();
			HashMap<String, HashMap<String, Double>> r_f_p = entry.getValue();
			for (Map.Entry<String, HashMap<String, Double>> entry1 : r_f_p.entrySet()) {
				String right = entry1.getKey();
				HashMap<String, Double> f_p = entry1.getValue();
				for (Map.Entry<String, Double> entry2 : f_p.entrySet()) {
					String f = entry2.getKey();
					double p = entry2.getValue();
					double p_update = p / getSumforEachLeft.get(f);
					f_p.put(f, p_update);
				}
			}
		}

		for (Map.Entry<String, HashMap<String, Double>> entry : unaries.entrySet()) {
			String left = entry.getKey();
			HashMap<String, Double> f_p = entry.getValue();
			for (Map.Entry<String, Double> entry1 : f_p.entrySet()) {
				String f = entry1.getKey();
				double p = entry1.getValue();
				double p_update = p / getSumforEachLeft.get(f);
				f_p.put(f, p_update);
			}
		}

		PrintWriter pw = new PrintWriter(output);
		Scanner sc_input = new Scanner(input);
		int index = 0;
		while (sc_input.hasNextLine()) {
			// System.out.println(index++);
			String sentence = sc_input.nextLine().trim();
			String[] words = sentence.split(" ");
			if (words.length <= 25) {
				HashMap[][] parseChart = new HashMap[words.length + 1][words.length + 1];
				for (int i = 0; i < parseChart.length; i++) {
					for (int j = 0; j < parseChart[0].length; j++) {
						parseChart[i][j] = new HashMap();
					}
				}

				for (int i = 0; i <= words.length - 1; i++) {
					C c_word = new C(words[i].trim(), null, null, 1.0, 1);
					addEntries(unaries, c_word, parseChart[i][i + 1]);
				}

				for (int k = 1; k <= words.length; k++) {// step-width
					for (int i = 0; i <= words.length - k; i++) {// start loc
						for (int j = i + 1; j <= i + k - 1; j++) {// split loc

							for (Object o1 : parseChart[i][j].keySet()) {
								String left = (String) o1;
								C c1 = (C) parseChart[i][j].get(o1);
								if (binaries.containsKey(left)) {
									HashMap<String, HashMap<String, Double>> poss_right = binaries.get(left);
									for (String right : poss_right.keySet()) {
										Object o2 = (Object) right;
										if (parseChart[j][i + k].containsKey(o2)) {
											C c2 = (C) parseChart[j][i + k].get(o2);
											HashMap<String, Double> f_p = binaries.get(left).get(right);
											for (Map.Entry<String, Double> sd : f_p.entrySet()) {
												String father = sd.getKey();
												double p = sd.getValue();
												C temp_c = new C(father, c1, c2, p * c1.getP() * c2.getP(), 2);
												addEntries(unaries, temp_c, parseChart[i][i + k]);
											}
										}
									}
								}
							}
						}
					}
				}
				traverse((C) parseChart[0][words.length].get("TOP"), pw);
			} else {
				pw.print("*IGNORE*");
			}
			pw.println("");
			pw.flush();
		}
		sc_input.close();
		pw.close();
	}

	private static void traverse(C c, PrintWriter pw) {
		if (c.getLeft() == null && c.getRight() == null) {
			// if c is a word
			pw.print(" " + c.getLabel());
		}
		if (c.getLeft() != null && c.getRight() == null) {
			// if c is unary
			pw.print(" (" + c.getLabel());
			traverse(c.getLeft(), pw);
			pw.print(")");
		}
		if (c.getLeft() != null && c.getRight() != null) {
			// if binarys_
			pw.print(" (" + c.getLabel());
			traverse(c.getLeft(), pw);
			traverse(c.getRight(), pw);
			pw.print(")");
		}
		if (c.getLeft() == null && c.getRight() != null) {
			System.out.println("BigMistake");
		}
	}

	private static void addEntries(HashMap<String, HashMap<String, Double>> unaries, C to_add, HashMap parseChart) {

		if (parseChart.values().contains(to_add) == false) {
			// add in thread
			if (parseChart.keySet().contains(to_add.getLabel()) == false) {
				parseChart.put(to_add.getLabel(), to_add);
				if (unaries.containsKey(to_add.getLabel())) {
					HashMap<String, Double> poss_left_p = unaries.get(to_add.getLabel());
					for (Map.Entry<String, Double> entry : poss_left_p.entrySet()) {
						String left = entry.getKey();
						double p = entry.getValue();
						C uni_c = new C(left, to_add, null, p * to_add.getP(), 1);
						addEntries(unaries, uni_c, parseChart);
					}
				}
			} else {
				C cur_c = (C) parseChart.get(to_add.getLabel());
				if (cur_c.getP() <= to_add.getP()) {
					parseChart.put(to_add.getLabel(), to_add);
					if (unaries.containsKey(to_add.getLabel())) {
						HashMap<String, Double> poss_left_p = unaries.get(to_add.getLabel());
						for (Map.Entry<String, Double> entry : poss_left_p.entrySet()) {
							String left = entry.getKey();
							double p = entry.getValue();
							C uni_c = new C(left, to_add, null, p * to_add.getP(), 1);
							addEntries(unaries, uni_c, parseChart);
						}
					}
				}
			}
		} 
	}
}
