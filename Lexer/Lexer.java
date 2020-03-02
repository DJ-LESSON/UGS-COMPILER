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
 * compiler lesson assignment 1: Lexical analysis
 * the author is AnDJ from XXXXX
 * which student number is XXXXX
 * date: 16 Dec, 2018
 * Github: https://github.com/An-DJ
 */
import java.util.*;

// class 1: graph node class which can be used to NFA and DFA.
class GraphNode{
    // sequence number for this node in specific graph.
    public int sequence_num;
    // used for visit the specific graph.
    public boolean visited;
    // children nodes stored by tuple of (node,condition) which means "this ---(condition)---> specific child".
    public HashMap<GraphNode,Character> children;
    // used for simplifying the nfa. this node can be replaced by its equal node.
    public GraphNode equalNode;

    public GraphNode(){
        children=new HashMap<>();
        sequence_num=0;
        visited=false;
    }

    // add a child node.
    public void addChild(GraphNode node,char condition){
        children.put(node,condition);
    }
    // set the sequence number.
    public void setSequence_num(int num){
        sequence_num=num;
    }

    @Override
    public String toString() {
        return String.valueOf(sequence_num);
    }

    // print the situation of this node.
    public void print(){
        System.out.println("node:"+sequence_num);
        System.out.println("\t"+children);

    }
}
// class 2: Nondeterministic finite automaton
// this class store a nfa and the methods for operating the nfa.
class NFA{
    // start node and end node.
    public GraphNode startNode;
    public GraphNode endNode;
    // construct a nfa like "start---(epsilon)--->end"
    public NFA(){
        startNode=new GraphNode();
        endNode=new GraphNode();
        startNode.addChild(endNode,(char)0);
    }
    // construct a nfa by given start and end node.
    public NFA(GraphNode startNode,GraphNode endNode){
        this.startNode=startNode;
        this.endNode=endNode;
    }
    // construct a nfa like "start---(condition)--->end"
    public NFA(char condition){
        startNode=new GraphNode();
        endNode=new GraphNode();
        startNode.addChild(endNode,condition);
    }
    // add two nfas and create a new one.
    // start1---(omit)--->end1---(epsilon)--->start2---(omit)--->end2
    //   ^                                                        ^
    //   |                                                        |
    //  start                                                    end
    public static NFA addNFA(NFA nfa1,NFA nfa2){
        nfa1.endNode.addChild(nfa2.startNode,(char)0);
        return new NFA(nfa1.startNode,nfa2.endNode);
    }
    // "or" operation for two nfas and create a new one.
    // newStartNode---(epsilon)--->start1---(omit)--->end1---(epsilon)--->newEndNode
    //      |                                                                  ^
    //      |                                                                  |
    //      +---------(epsilon)--->start2---(omit)--->end2---(epsilon)---------+
    public static NFA orNFA(NFA nfa1,NFA nfa2){
        NFA newNFA=new NFA(new GraphNode(),new GraphNode());
        newNFA.startNode.addChild(nfa1.startNode,(char)0);
        nfa1.endNode.addChild(newNFA.endNode,(char)0);
        newNFA.startNode.addChild(nfa2.startNode,(char)0);
        nfa2.endNode.addChild(newNFA.endNode,(char)0);
        return newNFA;
    }
    // "*" operation for single nfa and create a new one.
    //    newStartNode---(epsilon)--->newEndNode
    //         |    ^
    //         |     \
    //(epsilon)|      +------------------+(epsilon)
    //         v                          \
    //        start-------(omit)--------->end
    public static NFA starNFA(NFA nfa){
        NFA newNFA=new NFA();
        newNFA.startNode.addChild(nfa.startNode,(char)0);
        nfa.endNode.addChild(newNFA.startNode,(char)0);
        return newNFA;
    }
}
// class 3: nfa cluster item which is stand for the item in nfa's table like this:
//  +-------------------------------------------------------------+
//  |                       |   condition1     |   condition2     |
//  +-------------------------------------------------------------+
//  |clusterItem1(1*,2,3*,5)|clusterItem2(3*,5)|clusterItem3(4*,2)|
//  +-------------------------------------------------------------+
//  |clusterItem2(3*,5)     |clusterItem2(3*,5)|clusterItem3(4*,2)|
//  +-------------------------------------------------------------+
// An item also corresponds to a non-simplified dfa node.
class NFAClusterItem
        implements Comparable<NFAClusterItem>{
    // the all node in cluster item like (1,2,3,5) for clusterItem1(1*,2,3*,5) above.
    List<GraphNode> nodes;
    // the head node in cluster item like (1,3) for clusterItem1(1*,2,3*,5) above.
    // that means the non-head nodes can be reach by 'epsilon' condition from head nodes.
    List<GraphNode> headNodes;
    // dfa number for a specific dfa.
    public int dfaNum;
    // this variable marks whether this non-simplified dfa node is end node.
    public boolean isEndNode;

    // construct a cluster item.
    // parameter node for head which will be add.
    // parameter endNode for judge whether this non-simplified dfa node is end node.
    public NFAClusterItem(GraphNode node, GraphNode endNode){
        nodes=new ArrayList<>();
        headNodes=new ArrayList<>();
        dfaNum=0;
        isEndNode=false;
        if (node==endNode)
            isEndNode=true;
        nodes.add(node);
        headNodes.add(node);
        addNoneEpsilonChild(node,endNode);
    }

    public NFAClusterItem(){
        nodes=new ArrayList<>();
        headNodes=new ArrayList<>();
        dfaNum=0;
        isEndNode=false;
    }

    @Override
    public String toString() {
        return nodes.toString();
    }

    // set the number of non-simplified dfa node.
    public NFAClusterItem setDfaNum(int num){
        dfaNum=num;
        return this;
    }
    // add a non-epsilon condition headNode.
    // recursively add as many as non-epsilon condition node it can reach.
    // parameter endNode for judge this cluster like above.
    public void addNoneEpsilonNode(GraphNode headNode,GraphNode endNode){
        if (headNode==endNode)
            isEndNode=true;
        headNodes.add(headNode);
        nodes.add(headNode);
        addNoneEpsilonChild(headNode,endNode);
    }
    // add a non-epsilon condition node.
    // recursively add as many as non-epsilon condition node it can reach.
    // parameter endNode for judge this cluster like above.
    private void addNoneEpsilonChild(GraphNode node,GraphNode endNode){
        GraphNode []childs=new GraphNode[node.children.size()];
        node.children.keySet().toArray(childs);
        for (int i=0;i<node.children.size();i++){
            if(node.children.get(childs[i])==0){
                nodes.add(childs[i]);
                if(childs[i]==endNode)
                    isEndNode=true;
                addNoneEpsilonChild(childs[i],endNode);
            }
        }
        return;
    }
    // hash code used for hashMap.
    // it is not good useful. we should rewrite the equals function.
    @Override
    public int hashCode() {
        return nodes.get(0).hashCode();
    }

    // judge whether two cluster items are equal with each other.
    // only should compare the headNodes. That's enough.
    @Override
    public boolean equals(Object obj) {
        boolean isEqual=true;
        NFAClusterItem item=(NFAClusterItem)obj;
        if(headNodes.size()!=item.headNodes.size()) return false;
        for (int i=0;i<item.headNodes.size();i++){
            if(!headNodes.get(i).equals(item.headNodes.get(i))){
                isEqual=false;
                break;
            }
        }
        return isEqual;
    }
    // compare two non-simplified dfa nodes by dfa number.
    @Override
    public int compareTo(NFAClusterItem o) {
        if (dfaNum==o.dfaNum) return 0;
        return dfaNum>o.dfaNum?1:-1;
    }
}
// class 4: Lexer.
// Main class.
public class Lexer {
    // the regular expression.
    private String regex;
    // the nfa graph root node.
    private NFA nfaRoot;
    // the dfa graph root node.
    private GraphNode dfaRoot;
    // constructor for transform from regular expression to dfa.
    public Lexer(String regex){
        // 1) store regex.
        this.regex=regex;
        nfaRoot=new NFA();
        dfaRoot=null;
        // 2) regex to rpn expression.
        regex2RPN(regex);
        // 3) rpn regex to nfa.
        regex2NFA(this.regex);
        // 4) number the nfa.
        System.out.println("\n2.-----------------NFA-------------------------\n");
        numberNFAorDFA(nfaRoot.startNode);
        // 5) nfa to dfa.
        nfa2DFA();
        // 6) number the dfa.
        System.out.println("\n4.-----------------DFA-------------------------\n");
        numberNFAorDFA(dfaRoot);
    }

    // regex to rpn expression.
    // * > + > |
    private void regex2RPN(String regex){
        Stack<Character> characterStack=new Stack<>();
        Stack<Character> notationStack=new Stack<>();
        notationStack.push('@');
        for (int i=0;i<regex.length();i++){
            if(regex.charAt(i)=='('){
                notationStack.push('(');
            }else if(regex.charAt(i)==')'){
                while (notationStack.peek()!='('){
                    characterStack.push(notationStack.pop());
                }
                notationStack.pop();
            }else if(regex.charAt(i)=='*'){
                notationStack.push('*');
            }else if(regex.charAt(i)=='+'){
                while (notationStack.peek()=='*'){
                    characterStack.push(notationStack.pop());
                }
                notationStack.push('+');
            }else if(regex.charAt(i)=='|'){
                while (notationStack.peek()=='*'||notationStack.peek()=='+'){
                    characterStack.push(notationStack.pop());
                }
                notationStack.push('|');
            }else{
                characterStack.push(regex.charAt(i));
            }
        }
        while (!notationStack.empty()){
            characterStack.push(notationStack.pop());
        }
        Character[]chars=new Character[characterStack.size()];
        characterStack.toArray(chars);
        char [] re=new char[chars.length];
        for (int i=0;i<re.length;i++){
            re[i]=chars[i];
        }
        this.regex=String.valueOf(re);
        System.out.println("1.--------------regex to RPN-------------------\n\n\t"+this.regex.substring(0,this.regex.length()-1));
    }

    // rpn regex to non-numbered nfa.
    private void regex2NFA(String regex){
        Stack<NFA> nfaStack=new Stack<>();
        nfaStack.push(nfaRoot);
        for(int i=0;i<regex.length()-1;i++){
            if(regex.charAt(i)=='*'){
                NFA nfa=nfaStack.pop();
                nfaStack.push(NFA.starNFA(nfa));
            }else if(regex.charAt(i)=='+'){
                NFA nfa2=nfaStack.pop();
                NFA nfa1=nfaStack.pop();
                nfaStack.push(NFA.addNFA(nfa1,nfa2));
            }else if(regex.charAt(i)=='|'){
                NFA nfa2=nfaStack.pop();
                NFA nfa1=nfaStack.pop();
                nfaStack.push(NFA.orNFA(nfa1,nfa2));
            }else{
                NFA nfa=new NFA(regex.charAt(i));
                nfaStack.push(nfa);
            }
        }
        nfaRoot=nfaStack.peek();
    }
    // number the nfa nodes.
    // visit and print all nodes by BFS(Breadth-First-Search).
    private void numberNFAorDFA(GraphNode node){
        Queue<GraphNode> queue=new LinkedList<>();
        queue.offer(node);
        queue.peek().visited=true;
        int num=0;
        while (!queue.isEmpty()){
            GraphNode curretNode=queue.poll();
            curretNode.sequence_num=num++;
            for (int i=0;i<curretNode.children.size();i++){
                GraphNode child=(GraphNode) curretNode.children.keySet().toArray()[i];
                if(!child.visited){
                    child.visited=true;
                    queue.offer(child);
                }
            }
        }

        queue.offer(node);
        queue.peek().visited=false;
        while (!queue.isEmpty()){
            GraphNode curretNode=queue.poll();
            System.out.print(curretNode.sequence_num+": ");
            for (int i=0;i<curretNode.children.size();i++){
                GraphNode child=(GraphNode) curretNode.children.keySet().toArray()[i];
                System.out.print("("+curretNode.children.get(child)+","+child.sequence_num+") ");
                if(child.visited){
                    queue.offer(child);
                    child.visited=false;
                }

            }
            System.out.println();
        }
    }
    // nfa to dfa.
    private void nfa2DFA(){
        // the "nfa to dfa" table is like this:
        //  +-------------------------------------------------------------+
        //  |                       |   condition1     |   condition2     |
        //  +-------------------------------------------------------------+
        //  |clusterItem1(1*,2,3*,5)|clusterItem2(3*,5)|clusterItem3(4*,2)|
        //  +-------------------------------------------------------------+
        //  |clusterItem2(3*,5)     |clusterItem2(3*,5)|clusterItem3(4*,2)|
        //  +-------------------------------------------------------------+
        // each row can be a key-value pair like: (NFAClusterItem,HashMap(condition,NFAClusterItem))
        // each column for each row can be a key-value pair like: (condition,NFAClusterItem)
        HashMap<NFAClusterItem,HashMap<Character, NFAClusterItem>> nfaTable=new HashMap<>();
        int dfaNumber=0;
        // put the start node.create a new row in the table.
        nfaTable.put(new NFAClusterItem(nfaRoot.startNode,nfaRoot.endNode).setDfaNum(dfaNumber++),new HashMap<>());
        // extract the keys of this table.
        Object []keys=new Object[nfaTable.keySet().size()];
        nfaTable.keySet().toArray(keys);
        // the length of the table.
        int len=keys.length;


        for(int m=0;m<len;m++){
            // finish single row in table.
            //  +-------------------------------------------------------------+
            //  |                       |   condition1     |   condition2     |
            //  +-------------------------------------------------------------+
            //  |currentClusterItem     |      ?(fill it)  |      ?(fill it)  |
            //  +-------------------------------------------------------------+
            Arrays.sort(keys);
            NFAClusterItem item =(NFAClusterItem)keys[m];
            HashMap<Character,NFAClusterItem> map=nfaTable.get(item);
            for(int i = 0; i< item.nodes.size(); i++){
                for(GraphNode nextNode: item.nodes.get(i).children.keySet()){
                    char condition= item.nodes.get(i).children.get(nextNode);
                    if(condition!=0){
                        if(map.containsKey(condition)){
                            map.get(condition).addNoneEpsilonNode(nextNode,nfaRoot.endNode);
                        }else {
                            map.put(condition,new NFAClusterItem(nextNode,nfaRoot.endNode));
                        }
                    }
                }
            }
            // add new row in table if a new cluster node created above.
            //  +-------------------------------------------------------------+
            //  |                       |   condition1     |   condition2     |
            //  +-------------------------------------------------------------+
            //  |    newClusterItem(?)  |                  |                  |
            //  +-------------------------------------------------------------+
            Iterator<Character> iterator=map.keySet().iterator();
            while (iterator.hasNext()){
                NFAClusterItem clusterItem=map.get(iterator.next());
                if(!nfaTable.containsKey(clusterItem)){
                    clusterItem.setDfaNum(dfaNumber++);
                    nfaTable.put(clusterItem,new HashMap<>());
                }else {
                    Iterator<NFAClusterItem> iterator1=nfaTable.keySet().iterator();
                    while (iterator1.hasNext()){
                        NFAClusterItem findItem=iterator1.next();
                        if(findItem.equals(clusterItem)){
                            clusterItem.setDfaNum(findItem.dfaNum);
                        }
                    }
                }
            }
            keys=nfaTable.keySet().toArray();
            len=keys.length;
        }
        Arrays.sort(keys);
        // create the dfa graph.
        // number all dfa nodes and print the non-simplified dfa nodes.
        System.out.println("\n3.------------the dfa to nfa table-------------\n");
        GraphNode []nodes=new GraphNode[keys.length];
        for (int i=0;i<nodes.length;i++){
            nodes[i]=new GraphNode();
            nodes[i].setSequence_num(i);
            System.out.println(i+": "+((NFAClusterItem)keys[i]).nodes.toString()+" "+((NFAClusterItem)keys[i]).isEndNode);
            System.out.println(nfaTable.get(keys[i]).toString());
            System.out.println();
        }
        // find  all nodes' equal node which can replace itself.
        for(int i=0;i<keys.length;i++){
            for(int j=0;j<=i;j++){
                if(((NFAClusterItem)keys[i]).isEndNode==((NFAClusterItem)keys[j]).isEndNode
                        && isEqual(nfaTable.get(keys[i]),nfaTable.get(keys[j]))){
                    nodes[i].equalNode=nodes[j];
                    break;
                }
            }
        }
        // simplified the dfa by every nodes' equal node.
        for(int i=0;i<keys.length;i++){
            for (int j=0;j<nfaTable.get(keys[i]).size();j++){
                char condition=(Character)nfaTable.get(keys[i]).keySet().toArray()[j];
                Character c=Character.valueOf(condition);
                int num=nfaTable.get(keys[i]).get(c).dfaNum;
                nodes[i].addChild(nodes[num].equalNode,condition);
            }
        }
        dfaRoot=nodes[0];
    }
    // judge whether a table row equals to another row.
    public static boolean isEqual(HashMap<Character,NFAClusterItem> hashMap1,HashMap<Character,NFAClusterItem> hashMap2){
        int length=hashMap1.size()-hashMap2.size();
        if(length!=0) return false;
        length=hashMap1.size();
        boolean isEqual=true;
        Object []keys=hashMap1.keySet().toArray();
        for (int i=0;i<length;i++){
            if(!hashMap1.get(keys[i]).equals(hashMap2.get(keys[i]))){
                isEqual=false;
                break;
            }
        }
        return isEqual;
    }
    public static void main(String[] args){
        Scanner scanner=new Scanner(System.in);
        String regex=scanner.next();
        Lexer lexer=new Lexer(regex);
        //Lexer lexer=new Lexer("1+(0*+1)*|0+(1*+0)*");
        //Lexer lexer=new Lexer("a+(a|b)*+a");
        //Lexer lexer=new Lexer("(a|b)*+a+(a|b)+(a|b)");
        //Lexer lexer=new Lexer("a*+b+a*+b+a*+b+a*");
    }
}
