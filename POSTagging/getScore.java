import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;


public class getScore {
	
	public static void main(String[] args) throws FileNotFoundException{
		 File file1 = new File(args[0]);
		 File file2 = new File(args[1]);
 
		
		Scanner scanner1 = new Scanner(new BufferedReader(new FileReader(file1)));
		Scanner scanner2 = new Scanner(new BufferedReader(new FileReader(file2)));
		
		double hit = 0.0;
		double sum = 0.0;
		while(scanner1.hasNextLine() && scanner2.hasNextLine()){
			String line1 = scanner1.nextLine().trim();
			String line2 = scanner2.nextLine().trim();
			String[] temp1 = line1.split(" ");
			String[] temp2 = line2.split(" ");

			sum+=(temp1.length/2);
			
			for (int i = 1; i <= temp1.length - 1; i = i + 2) {
				if(temp1[i].equals(temp2[i])){
					hit++;
				}
			}
		}
		System.out.println(hit/sum);
		scanner1.close();
		scanner2.close();
	}
	

}
