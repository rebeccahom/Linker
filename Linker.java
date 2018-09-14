import java.util.*;
import java.io.*;

public class Linker {
	public static void main(String args[]) throws FileNotFoundException {
		ArrayList<String> symbols = new ArrayList<String>();
		//Get the input
		try{
			//Read the file
			File userFile = new File(args[0]);
			Scanner fileReader = new Scanner(userFile);

			int defNum; //Number of definitions
			int useNum; //Number of uses (i.e. external references)
			int instNum; //Number of program text
			int baseAddress = 0; //Base address of the module
			int absAddress = 0; //Absolute address; used to build symbol table
			int modNum = Integer.parseInt(fileReader.next()); //Number of modules in the input
			ArrayList<String> defErrorMsg = new ArrayList<String>(); //Used to check if an address appearing in a definition exceeds the module size
			int printCheck = 0;
			Module[] modules = new Module[modNum]; //Initialize the modules

			//Insert the input into the modules
			for (int i = 0; i < modNum; i++){
				modules[i] = new Module(); //Create a new module
				modules[i].base = baseAddress; //Update its base address

				defNum = fileReader.nextInt();
				for (int j = 0; j < defNum*2; j++){
					modules[i].definitions.add(fileReader.next());
				}

				useNum = fileReader.nextInt();
				for (int k = 0; k < useNum; k++){
					modules[i].uses.add(fileReader.next());
				}

				instNum = fileReader.nextInt();
				for (int l = 0; l < instNum*2; l++){
					modules[i].instruction.add(fileReader.next());
				}

				//Adding values to the symbol table
				if (modules[i].definitions.isEmpty() != true){
					for (int x = 0; x < modules[i].definitions.size(); x+=2){
						//Check if the symbol is already defined; if it is, then remove it
						if (symbols.contains(modules[i].definitions.get(x))){
							System.out.println(modules[i].definitions.get(x) + " is multiply defined; first value is used");
						}

						//If the symbol is not defined in the symbol table, add it 
						else{
							symbols.add(modules[i].definitions.get(x));
							//If the symbol's address exceeds the module's size, use the value 0
							if (Integer.parseInt(modules[i].definitions.get(x+1)) > instNum){
								symbols.add(Integer.toString(modules[i].base));
								defErrorMsg.add("Warning: In module " + i + " the definition of " + modules[i].definitions.get(x) + " exceeds the module size; zero (relative) used.");
							}

							else{
								absAddress = Integer.parseInt(modules[i].definitions.get(x+1)) + modules[i].base;
								symbols.add(Integer.toString(absAddress)); 
							}
						}
					}
				}
				
				baseAddress += instNum;
			}
			fileReader.close();
			//Print out the symbol table
			System.out.println("Symbol table");
			for (int i = 0; i<symbols.size(); i+=2){
				System.out.println(symbols.get(i) + " = " + symbols.get(i + 1));
			}

			//Error checking for having symbols in use list but not actually using them
			ArrayList<String> useCopy = new ArrayList<String>();
			ArrayList<String> useCopyErrorMsg = new ArrayList<String>();

			//Print out the memory map
			int lineCount = 0;
			System.out.println("\nMemory map");

			//Go through each module and resolve each external reference
			for (int i = 0; i < modules.length; i++){
				for (int useCopyIterator = 0; useCopyIterator < useCopy.size(); useCopyIterator++){
					useCopyErrorMsg.add("Warning: In module " + (i-1) + " " + useCopy.get(useCopyIterator) + " appeared in the use list but was not actually used");
				}
				useCopy.clear();
				
				for (int i2 = 0; i2 < modules[i].uses.size(); i2++){
					useCopy.add(modules[i].uses.get(i2));
				}

				//Iterate over the instruction list
				for (int j = 0; j < modules[i].instruction.size(); j++){
					//The types are placed in the even indices; the instructions are placed in odd indices
					if (j%2 == 0){ 
						//Relocate the relative address
						if (modules[i].instruction.get(j).equals("R")){
							//Check if the relative address exceeds the module size
							String relativeCheck = modules[i].instruction.get(j+1);
							//RELATIVE ADDRESS ERROR							
							if (Integer.parseInt(relativeCheck)%1000 > modules[i].instruction.size()){
								modules[i].instruction.set(j + 1, modules[i].instruction.get(j+1).substring(0,1) + "000");
								System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1) + " Error: Relative address exceeds module size; zero used");
							}
							else{
								int tempInstruction = Integer.parseInt(modules[i].instruction.get(j+1)) + modules[i].base;							
								modules[i].instruction.set(j + 1, Integer.toString(tempInstruction));
								System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1));
							}


						}

						//Resolve the external reference
						else if (modules[i].instruction.get(j).equals("E")){	
							//Check if the symbol is actually defined
							for (int k = 0; k < modules[i].uses.size(); k++){
								//If the symbol is used, remove it
								useCopy.remove(modules[i].uses.get(k));
								if (symbols.contains(modules[i].uses.get(k)) != true){
									String newInstruction = modules[i].instruction.get(j+1).substring(0,1) + "000";
									modules[i].instruction.set(j + 1, newInstruction);
									System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1) + " Error: " + modules[i].uses.get(k) + " is not defined; zero used.");
									printCheck++;
								}
							}
							if (printCheck <= 0){
								//Get the index (the last digit of the instruction)
								int tempIndex = Integer.parseInt(modules[i].instruction.get(j+1))%1000; //use %1000 to get the index

								//If the index is not within the range of the use list, then treat the address as immediate
								if (tempIndex > modules[i].uses.size()){
									System.out.println(lineCount + ":  " + modules[i].instruction.get(j + 1) + " Error: External address exceeds length of use list; treated as immediate");
									modules[i].instruction.set(j,"I");}
								else{
									//Go to the module's external reference list
									String symbol = modules[i].uses.get(tempIndex);
									//If the symbol is used, remove it
									useCopy.remove(symbol);

									//Assign that symbol its value
									String symbolValue = symbols.get(symbols.indexOf(symbol) + 1);

									//Update the module with the symbol value
									if (symbolValue.length() < 2){
										String newInstruction = modules[i].instruction.get(j+1).substring(0, 1) + "00" + symbolValue;
										modules[i].instruction.set(j + 1, newInstruction);
										System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1));}
									else if (symbolValue.length() < 3){
										String newInstruction = modules[i].instruction.get(j+1).substring(0,1) + "0" + symbolValue;
										modules[i].instruction.set(j + 1, newInstruction);
										System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1));}
									else{
										String newInstruction = modules[i].instruction.get(j+1).substring(0, 1) + symbolValue;
										modules[i].instruction.set(j + 1, newInstruction);
										System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1));}
								}
							}
							printCheck = 0;
						}

						else if (modules[i].instruction.get(j).equals("A")){
							//Check if the absolute address exceeds the size of the machine
							String tempInstruction = modules[i].instruction.get(j + 1).substring(1);
							int checkTempInstruction = Integer.parseInt(tempInstruction);
							if (checkTempInstruction >= 200){
								String newInstruction = modules[i].instruction.get(j+1).substring(0,1) + "000";
								modules[i].instruction.set(j + 1, newInstruction);
								System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1) + " Error: Absolute address exceeds machine size; zero used");
							}
							else{
								System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1));
							}
						}

						else{
							System.out.println(lineCount + ":  " + modules[i].instruction.get(j+1));
						}

						lineCount++;
					}
				}
			}

			/*Check if a symbol is defined but not used
			Go through the definitions list and see if any elements are missing from the use list*/
			ArrayList<String> check = new ArrayList<String>();
			for (int i = 0; i<modNum; i++){
				//Populate the array list with the defined symbols
				for (int j = 0; j < modules[i].definitions.size(); j+=2){
					check.add(modules[i].definitions.get(j)); //Save the defined element
					check.add(Integer.toString(i)); //Save the module in which that element is stored
				}				
			}

			//Check if all the defined symbols are used at least once in all of the modules
			for (int i = 0; i<modNum; i++){
				for (int j = 0; j < modules[i].uses.size(); j++){
					//Since the symbol is in the list, remove it from the check list
					while (check.contains(modules[i].uses.get(j))){
						int tempIndex = check.indexOf(modules[i].uses.get(j));
						check.remove(modules[i].uses.get(j));
						check.remove(tempIndex);
					}
				}
			}	
			//Iterate through the check list and see if any modules are left
			System.out.println();
			if (check.size() > 0){
				for (int i = 0; i<check.size(); i+=2){
					System.out.println("Warning: " + check.get(i) + " was defined in module " + check.get(i + 1) + " but was never used.");
				}
			}

			if (useCopyErrorMsg.size() > 0){
				for (int i = 0; i<useCopyErrorMsg.size(); i++){
					System.out.println(useCopyErrorMsg.get(i));
				}
			}

			if (defErrorMsg.size() > 0){
				for (int i = 0; i<defErrorMsg.size();i++){
					System.out.println(defErrorMsg.get(i));
				}
			}
		}

		catch(FileNotFoundException error){
			System.err.println("Error: The file \"" + args[0] + "\" cannot be opened.");
		}
	}
}

class Module {
	int base;
	ArrayList<String> definitions;
	ArrayList<String> uses;
	ArrayList<String> instruction;

	public Module() {
		base = 0;
		definitions = new ArrayList<String>();
		uses = new ArrayList<String>();
		instruction = new ArrayList<String>();
	}
}