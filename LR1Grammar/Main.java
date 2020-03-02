/**
 * MIT License
 *
 * Copyright (c) 2018 AnDJ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/**
 * compiler lesson assignment 3: LR(1) grammar analysis
 * the author is AnDJ from XXXXXX
 * whose student number is XXXXXX
 * date: 27 Dec, 2018
 * Github: https://github.com/An-DJ
 */
import java.util.*;

class Production{
    public char leftPart;
    public String rightPart;
    public int index;
    public boolean isReduce;
    public HashSet<Character> lookAhead;
    public Production(char leftPart,String rightPart){
        this.leftPart=leftPart;
        this.rightPart=rightPart;
        lookAhead=new HashSet<>();
        index=0;
        isReduce=false;
    }
    public Production(String input){
        String []strs=input.split("->");
        if (strs.length!=2) return;
        leftPart=strs[0].charAt(0);
        rightPart=strs[1];
        lookAhead=new HashSet<>();
        index=0;
        isReduce=false;
    }

    public Production(){
        this(' ',"");
    }
    public Production addLookAhead(char ahead){
        lookAhead.add(ahead);
        return this;
    }
    public static Production createOneStepProduction(Production production){
        Production p=new Production(production.leftPart,production.rightPart);
        p.lookAhead.addAll(production.lookAhead);
        p.index=production.index+1;
        return p;
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
    public static Production createNewProduction(Production production){
        return new Production(production.leftPart,production.rightPart);
    }

    @Override
    public String toString() {
        StringBuilder stringBuffer=new StringBuilder();
        stringBuffer.append("\t").append(leftPart)
                .append("->")
                .append(rightPart, 0, index)
                .append(" ").append(rightPart.substring(index))
                .append("    ").append(lookAhead.toString())
                .append("\n\t   ");
        for (int i=0;i<index;i++) stringBuffer.append(" ");
        stringBuffer.append("^");
        return stringBuffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        Production production=(Production)obj;
        boolean r=leftPart==production.leftPart &&
                rightPart.equals(production.rightPart) &&
                index==production.index &&
                lookAhead.equals(production.lookAhead);
        return r;
    }

    @Override
    public int hashCode() {
        return 0;
    }
    public boolean isLike(Production production){
        return leftPart==production.leftPart &&
                rightPart.equals(production.rightPart) &&
                index==production.index;
    }
}
class GraphNode{
    public int sequence_num;
    public boolean visit;
    public List<GraphNode> parents;
    public LinkedHashSet<Production> productions;
    public HashMap<Character,GraphNode> children;
    public GraphNode(){
        productions=new LinkedHashSet<>();
        children=new HashMap<>();
        parents=new ArrayList<>();
    }

    public void setSequence_num(int sequence_num) {
        this.sequence_num = sequence_num;
    }

    @Override
    public String toString() {
        StringBuilder s= new StringBuilder("Node "+String.valueOf(sequence_num)+":");
        for (Production production:productions)
            s.append("\n").append(production.toString());
        return s.toString();
    }
    public void addProduction(Production production){
        for (Production p:productions){
            if(production.isLike(p)){
                p.lookAhead.addAll(production.lookAhead);
                return;
            }
        }
        productions.add(production);
    }
    public GraphNode addChild(char condition){
        if(children.containsKey(condition)) return children.get(condition);
        else {
            GraphNode child =new GraphNode();
            child.parents.add(this);
            children.put(condition,child);
            return child;
        }
    }

    @Override
    public boolean equals(Object obj) {
        GraphNode node=(GraphNode)obj;
        return productions.equals(node.productions);
    }

    @Override
    public int hashCode() {
        return 0;
    }
    public void print(){
        System.out.println("node "+sequence_num+":");
        for (Production production:productions){
            if (production.isReduce){
                System.out.println("\tReduce "+production.leftPart+"->"+production.rightPart+" on "+production.lookAhead.toString());
            }
        }
        for (char key:children.keySet()){
            System.out.println("\tvia "+key+" ==> "+children.get(key).sequence_num);
        }
    }
}
class Action{
    public String name;
    public int shiftNum;
    public int goNum;
    public Production reduceProduction;
    public static Action createShiftAction(int shiftNum){
        Action action=new Action();
        action.name="shift";
        action.shiftNum=shiftNum;
        return action;
    }
    public static Action createGoAction(int goNum){
        Action action=new Action();
        action.name="go";
        action.goNum=goNum;
        return action;
    }
    public static Action createReduceAction(Production production){
        Action action=new Action();
        action.name="reduce";
        action.reduceProduction=production;
        return action;
    }

    @Override
    public String toString() {
        switch (name) {
            case "shift":
                return name + " "+String.valueOf(shiftNum);
            case "go":
                return name + " "+String.valueOf(goNum);
            default:
                return name + " "+String.valueOf(reduceProduction.leftPart + "->" + reduceProduction.rightPart);
        }
    }
}
class Grammar {
    private char startNonTerminal;
    private char epsilon;
    private List<GraphNode> dfaNodes;
    private HashMap<Character,HashSet<String>> productions;
    private HashMap<Character,HashSet<Character>> firstBases;
    private List<HashMap<Character,Action>> parseTable;
    public Grammar(List<Production> list, char startNonTerminal, char epsilon){
        this.startNonTerminal=startNonTerminal;
        this.epsilon=epsilon;
        dfaNodes=new ArrayList<>();
        productions=new HashMap<>();
        firstBases=new HashMap<>();
        parseTable=new ArrayList<>();
        for (Production p:list){
            if (!productions.containsKey(p.leftPart)) {
                HashSet<String> set = new HashSet<>();
                productions.put(p.leftPart,set);
            }
            productions.get(p.leftPart).add(p.rightPart);
        }
        splitProductions();
        calculateFirst();
        calculateDFA();
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
    private HashSet<Character> getFirstBases(String str){
        HashSet<Character> bases=new HashSet<>();
        for(int i=0;i<str.length();i++){
            char c=str.charAt(i);
            //todo 1.epsilon
            if(c==epsilon)
                continue;
            if(!firstBases.containsKey(c)){
                bases.add(c);
                break;
            }
            bases.addAll(firstBases.get(c));
            if (!bases.contains(epsilon))
                break;
        }
        return bases;
    }
    public void printProductions(){
        System.out.println("-------------------------all productions");
        for (char left:productions.keySet()){
            System.out.println(left+":");
            System.out.println("\t"+productions.get(left).toString());
        }
    }
    public void printFirstBases(){
        System.out.println("-----------------------------------first");
        for (char left:firstBases.keySet()){
            System.out.println(left+":");
            System.out.println("\t"+firstBases.get(left).toString());
        }
    }
    private void calculateDFA(){
        dfaNodes.clear();
        Queue<GraphNode> nonSolvedNodeQueue=new LinkedList<>();
        GraphNode node=new GraphNode();
        node.addProduction(new Production((char)0,String.valueOf(startNonTerminal)).addLookAhead('$'));
        nonSolvedNodeQueue.offer(node);
        while (!nonSolvedNodeQueue.isEmpty()){
            GraphNode currentNode=nonSolvedNodeQueue.poll();
            for(int i=0;i<currentNode.productions.size();i++){
                Production production=(Production) currentNode.productions.toArray()[i];
                if(production.index==production.rightPart.length()) continue;
                char condition=production.rightPart.charAt(production.index);
                if(!firstBases.containsKey(condition))
                    continue;
                for (String rights:productions.get(condition)) {
                    Production p=new Production(condition,rights);
                    if(production.index+1==production.rightPart.length())
                        p.lookAhead.addAll(production.lookAhead);
                    else
                        p.lookAhead.addAll(getFirstBases(production.rightPart.substring(production.index+1)));
                    currentNode.addProduction(p);
                }
            }
            for (Production production:currentNode.productions){
                if(production.rightPart.length()==production.index) {
                    production.isReduce = true;
                    continue;
                }
                GraphNode newNode=currentNode.addChild(production.rightPart.charAt(production.index));
                newNode.addProduction(Production.createOneStepProduction(production));
            }
            GraphNode likeNode=null;
            for (GraphNode graphNode:dfaNodes){
                //todo
                if (graphNode.equals(currentNode)){
                    likeNode=graphNode;
                }
            }
            if (likeNode!=null){
                for (GraphNode parent:currentNode.parents){
                    for (char childKey:parent.children.keySet()){
                        if (parent.children.get(childKey)==currentNode)
                            parent.children.replace(childKey,likeNode);
                    }
                }
            }else{
                dfaNodes.add(currentNode);
                nonSolvedNodeQueue.addAll(currentNode.children.values());
            }

        }
        for (int i=0;i<dfaNodes.size();i++){
            dfaNodes.get(i).setSequence_num(i);
        }
    }
    private void calculateParseTable(){
        parseTable.clear();
        for (GraphNode dfaNode : dfaNodes) {
            HashMap<Character, Action> hashMap = new HashMap<>();
            HashMap<Character, GraphNode> children = dfaNode.children;
            for (Production production : dfaNode.productions) {
                if (production.isReduce) {
                    for (char lookahead : production.lookAhead)
                        hashMap.put(lookahead, Action.createReduceAction(production));
                }
            }
            for (char key : children.keySet()) {
                if (firstBases.containsKey(key))
                    hashMap.put(key, Action.createGoAction(children.get(key).sequence_num));
                else
                    hashMap.put(key, Action.createShiftAction(children.get(key).sequence_num));
            }
            parseTable.add(hashMap);
        }
    }
    public void printDFA(){
        System.out.println("-------------------------------------DFA");
        for (GraphNode node1:dfaNodes){
            System.out.println(node1.toString());
        }
    }
    public void printSimplifiedDFA(){
        System.out.println("--------------------------simplified DFA");
        for (GraphNode node:dfaNodes){
            node.print();
        }
    }
    public void printParseTable(){
        System.out.println("-----------------------------Parse Table");
        for (int i=0;i<parseTable.size();i++){
            System.out.print(i+" ");
            System.out.println(parseTable.get(i).toString());
        }
    }
    public void parse(String input){
        System.out.printf("%-20s%-20s%-20s%-20s\n","STATE_STACK","NOTATION_STACK","INPUT","ACTION");
        input=input+"$";
        Stack<Integer> graphNodeStack=new Stack<>();
        Stack<Character> notationStack=new Stack<>();
        graphNodeStack.push(0);
        notationStack.push('$');
        int string_index=0;
        boolean isAccepted=false;
        while (!graphNodeStack.isEmpty()&&input.length()!=string_index){
            if(notationStack.peek()==(char)0){
                isAccepted=true;
                break;
            }
            GraphNode node=dfaNodes.get(graphNodeStack.peek());
            char c=input.charAt(string_index);
            Action nowAction=parseTable.get(node.sequence_num).get(c);
            if(nowAction==null)
                break;
            System.out.printf("%-20s%-20s%-20s%-20s\n",graphNodeStack,notationStack,input.substring(string_index),nowAction.toString());
            switch (nowAction.name){
                case "shift":
                    notationStack.push(c);
                    string_index++;
                    graphNodeStack.push(nowAction.shiftNum);

                    break;
                case "reduce":
                    for (int i=0;i<nowAction.reduceProduction.rightPart.length();i++){
                        graphNodeStack.pop();
                        notationStack.pop();
                    }
                    notationStack.push(nowAction.reduceProduction.leftPart);
                    Action nextAction=parseTable.get(graphNodeStack.peek()).get(notationStack.peek());
                    while (nextAction!=null && nextAction.name.equals("go")){
                        System.out.printf("%-20s%-20s%-20s%-20s\n",graphNodeStack,notationStack,input.substring(string_index),nextAction.toString());
                        graphNodeStack.push(nextAction.goNum);
                        nextAction=parseTable.get(graphNodeStack.peek()).get(notationStack.peek());
                    }
                    break;
            }
        }
        if(isAccepted) System.out.printf("%-20s%-20s%-20s%-20s\n",graphNodeStack,notationStack,input.substring(string_index),"Accepted");
        else System.out.printf("%-20s%-20s%-20s%-20s\n",graphNodeStack,notationStack,input.substring(string_index),"Not Accepted");
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
            productions.add(new Production(scanner.next()));
            System.out.print(">");
        }
        Grammar grammar =new Grammar(productions,startNonTerminal,'Ɛ');
        grammar.printProductions();
        grammar.printFirstBases();
        grammar.printDFA();
        grammar.printSimplifiedDFA();
        grammar.printParseTable();
        System.out.print("3.INPUT THE SENTENCE:\n>");
        scanner.next();
        grammar.parse(scanner.next());
    }
}
//example 1 : start with E
//E->E+(E)|int
//input: i+(i)+(i)

//example 2 : start with S
//S->[B
//A->i|[B
//B->]|C
//C->A]|A,C
//input: [i,i]