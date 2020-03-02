import java.util.*;

class Production{
    public char leftPart;
    public String rightPart;
    public Production(char leftPart,String rightPart){
        this.leftPart=leftPart;
        this.rightPart=rightPart;
    }
    public Production(String input){
        String []strs=input.split("->");
        if (strs.length!=2) return;
        leftPart=strs[0].charAt(0);
        rightPart=strs[1];
    }

    public Production(){
        leftPart=' ';
        rightPart="";
    }
    public static Production[] splitProduction(Production production){
        String [] rights=production.rightPart.split("\\|");
        Production[] productions=new Production[rights.length];
        for (int i=0;i<productions.length;i++){
            productions[i]=new Production();
            productions[i].leftPart=production.leftPart;
            productions[i].rightPart=rights[i];
        }
        return productions;
    }

    @Override
    public String toString() {
        return leftPart+"->"+rightPart;
    }
}
class Grammar {
    public char startNonTerminal;
    public char epsilon;
    public HashMap<Character,HashSet<String>> productions;
    public HashMap<Character,HashSet<Character>> firstBases;
    public HashMap<Character,HashSet<Character>> followBases;
    public HashMap<Character,HashMap<Character,String>> parseTable;
    public Grammar(List<Production> list, char startNonTerminal, char epsilon){
        this.startNonTerminal=startNonTerminal;
        this.epsilon=epsilon;
        productions=new HashMap<>();
        firstBases=new HashMap<>();
        followBases=new HashMap<>();
        parseTable=new HashMap<>();
        for (Production p:list){
            if (!productions.containsKey(p.leftPart)) {
                HashSet<String> set = new HashSet<>();
                productions.put(p.leftPart,set);
            }
            productions.get(p.leftPart).add(p.rightPart);
        }
        splitProductions();
        removeRecursive1();
        removeRecursive2();
        calculateFirst();
        calculateFollow();
        calculateParseTable();
    }
    private void splitProductions(){
        for (char left:productions.keySet()){
            HashSet<String> newSet=new HashSet<>();
            for (String right:productions.get(left)){
                String[] strs=right.split("\\|");
                for (int i=0;i<strs.length;i++)
                    newSet.add(strs[i]);
            }
            productions.put(left,newSet);
        }
    }
    private void removeRecursive1(){
        Set<Character> keySet=productions.keySet();
        for(char left:keySet){
            removeRecursive1(left);
        }
    }
    private void removeRecursive1(char left){
        HashSet<String> rightsSet=productions.get(left);
        HashSet<String> recursiveSet=new HashSet();
        HashSet<String> nonRecursiveSet=new HashSet();

        for (String right:rightsSet){
            if(right.startsWith(String.valueOf(left))) recursiveSet.add(right);
            else nonRecursiveSet.add(right);
        }
        char newLeft=(char)( 256+left-'A');
        if(recursiveSet.size()!=0){
            HashSet<String> pSet=new HashSet<>();
            productions.put(newLeft,pSet);
            pSet.add(String.valueOf(epsilon));
            rightsSet.clear();
            for (String rs:nonRecursiveSet){
                rightsSet.add(rs+newLeft);
            }
            for (String rs:recursiveSet){
                pSet.add(rs.substring(1)+newLeft);
            }
        }
    }
    private void removeRecursive2(){
        Set<Character> lefts=productions.keySet();
        for(int i=0;i<lefts.size();i++){
            HashSet<String> rights=productions.get(lefts.toArray()[i]);
            HashSet<String> oldRights= (HashSet<String>) rights.clone();
            for (int j=0;j<i;j++){
                for (String right:oldRights){
                    String prefix=String.valueOf((char) lefts.toArray()[j]);
                    if(right.startsWith(prefix)){
                        rights.remove(right);
                        for (String newAddEnd:productions.get(prefix.charAt(0)))
                        rights.add(newAddEnd+right.substring(1));
                    }
                }
            }
            removeRecursive1((char)lefts.toArray()[i]);
        }
    }
    private void calculateFirst(){
        boolean changed=false;
        Set<Character> nonTerminalsSet=productions.keySet();
        Object[] nonTerminals=nonTerminalsSet.toArray();
        for(int i=0;i<nonTerminals.length;i++){
            firstBases.put((char)nonTerminals[i],new HashSet<>());
        }
        do{
            changed=false;
            for (Object nonTerminalObject:nonTerminals){
                char nonTerminal=(char)nonTerminalObject;
                for(char left:productions.keySet()){
                    for (String right:productions.get(left)){
                        int firstSize=0;
                        for (int i=0;i<right.length();i++) {
                            char str = right.charAt(i);
                            firstSize = firstBases.get(left).size();
                            if (!nonTerminalsSet.contains(str)){
                                firstBases.get(left).add(str);
                                break;
                            }
                            else {
                                if (firstBases.get(str).contains(epsilon)) {
                                    firstBases.get(str).remove(epsilon);
                                    firstBases.get(left).addAll(firstBases.get(str));
                                    firstBases.get(str).add(epsilon);
                                }else{
                                    firstBases.get(left).addAll(firstBases.get(str));
                                    break;
                                }
                            }

                        }
                        if (firstBases.get(left).size() != firstSize) changed = true;
                    }
                }
            }
        }while (changed);
    }
    private void calculateFollow(){
        boolean changed;
        Set<Character> nonTerminalsSet=productions.keySet();
        Object[] nonTerminals=nonTerminalsSet.toArray();
        for(int i=0;i<nonTerminals.length;i++){
            followBases.put((char)nonTerminals[i],new HashSet<>());
        }
        followBases.get(startNonTerminal).add('$');
        do{
            changed=false;
                for(char left:productions.keySet()){
                    for (String right:productions.get(left)){
                        char currentChar;
                        int followSize;

                        for(int i=0;i<right.length();i++){
                            currentChar=right.charAt(i);
                            if(nonTerminalsSet.contains(currentChar)){
                                followSize=followBases.get(currentChar).size();
                                if(i==right.length()-1){
                                    followBases.get(currentChar).addAll(followBases.get(left));
                                }

                                else{
                                    char nextChar=right.charAt(i+1);
                                    if(!nonTerminalsSet.contains(nextChar))
                                        followBases.get(currentChar).add(nextChar);
                                    else{
                                        if(firstBases.get(nextChar).contains(epsilon)){
                                            followBases.get(currentChar).addAll(followBases.get(nextChar));
                                            firstBases.get(nextChar).remove(epsilon);
                                            followBases.get(currentChar).addAll(firstBases.get(nextChar));
                                            firstBases.get(nextChar).add(epsilon);
                                        }else{
                                            followBases.get(currentChar).addAll(firstBases.get(nextChar));
                                        }

                                    }
                                }
                                if(followBases.get(currentChar).size()!=followSize) changed=true;
                            }

                        }
                    }
                }
        }while (changed);
    }
    private void calculateParseTable(){
        Set<Character> nonTerminalsSet=productions.keySet();
        for (char left:nonTerminalsSet){
            parseTable.put(left,new HashMap<>());
        }
        for(char left: productions.keySet()){
            for(String right:productions.get(left)){
                if(!nonTerminalsSet.contains(right.charAt(0)) && right.charAt(0)!=epsilon){
                    parseTable.get(left).put(right.charAt(0),right);
                }else {
                    if(right.charAt(0)==epsilon){
                        for (char terminal:followBases.get(left)){
                            parseTable.get(left).put(terminal,right);
                        }
                        continue;
                    }
                    for (char terminal:firstBases.get(right.charAt(0))){
                        if(terminal!=epsilon) parseTable.get(left).put(terminal,right);
                    }

                    if (productions.get(right.charAt(0)).contains(String.valueOf(epsilon))) {
                        for (char terminal:followBases.get(left)){
                            //if(terminal!=epsilon)
                            parseTable.get(left).put(terminal,right);
                        }
                    }
                }
            }
        }
    }
    public void parse(String input){
        System.out.println("------------------------------------------parse");
        System.out.printf("%-20s%-20s%-20s\n","STACK","INPUT","ACTION");
        Set<Character> nonTerminalsSet=productions.keySet();
        Stack<Character> stack=new Stack<>();
        input=input+'$';
        stack.push('$');
        stack.push(startNonTerminal);
        while (!stack.empty()){
            if(input.length()==0) break;
            if(!nonTerminalsSet.contains(stack.peek())){
                if(stack.peek()==input.charAt(0)){
                    System.out.printf("%-20s%-20s%-20s\n",stack.toString(),input,"Terminal");
                    stack.pop();
                    input=input.substring(1);
                }else{
                    break;
                }
            }else{
                String right=parseTable.get(stack.peek()).get(input.charAt(0));
                if(right==null){
                    break;
                }
                if(right.equals(String.valueOf(epsilon))){
                    System.out.printf("%-20s%-20s%-20s\n",stack.toString(),input,epsilon);
                    stack.pop();
                }

                else{
                    System.out.printf("%-20s%-20s%-20s\n",stack.toString(),input,right);
                    stack.pop();
                    char[] cs=right.toCharArray();
                    for (int i=cs.length-1;i>=0;i--){
                        stack.push(cs[i]);
                    }
                }
            }
        }
        if(stack.isEmpty()&&input.length()==0)
            System.out.printf("%-20s%-20s%-20s\n","","result:","Accept");
        else
            System.out.printf("%-20s%-20s%-20s\n","","result:","Not Accept");
    }
    public void printProductions(){
        System.out.println("-------------------------------------all productions");
        for (char left:productions.keySet()){
            System.out.println(left+":");
            System.out.println("\t"+productions.get(left).toString());
        }
    }
    public void printFirstBases(){
        System.out.println("----------------------------------------------first");
        for (char left:firstBases.keySet()){
            System.out.println(left+":");
            System.out.println("\t"+firstBases.get(left).toString());
        }
    }
    public void printFollowBases(){
        System.out.println("----------------------------------------------follow");
        for (char left:followBases.keySet()){
            System.out.println(left+":");
            System.out.println("\t"+followBases.get(left).toString());
        }
    }
    public void printParseTable(){
        System.out.println("------------------------------------------parse table");
        for (char nonTerminal:parseTable.keySet()){
            System.out.println(nonTerminal+":");
            System.out.println("\t"+parseTable.get(nonTerminal).toString());
        }
    }
}
public class Main {
    public static void main(String []args){
        Scanner scanner=new Scanner(System.in);
        System.out.print("1.INPUT THE START NON_TERMINAL:\n>");
        char startNonTerminal=scanner.next().charAt(0);
        System.out.print("2.INPUT THE GRAMMAR:(copy Ɛ from there)\n>");
        List<Production> productions=new ArrayList<>();
        while (!scanner.hasNext("end")){
//            Production[] procs=Production.splitProduction(new Production(scanner.next()));
//            productions.addAll(Arrays.asList(procs));
            productions.add(new Production(scanner.next()));
            System.out.print(">");
        }
        Grammar grammar =new Grammar(productions,startNonTerminal,'Ɛ');
        grammar.printProductions();
        grammar.printFirstBases();
        grammar.printFollowBases();
        grammar.printParseTable();
        System.out.print("3.INPUT THE SENTENCE:\n>");
        scanner.next();
        grammar.parse(scanner.next());
    }
}