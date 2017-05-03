package edu.stanford.cs276.util;

import edu.stanford.cs276.CandidateGenerator;
import edu.stanford.cs276.Config;
import edu.stanford.cs276.LanguageModel;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

class TrieNode implements Serializable {

  Map<Character, TrieNode> children = new TreeMap<Character, TrieNode>();// don't need that much space
  int wordCount;

  //  Set<Integer> possiblePosition= new HashSet<Integer>();
  public TrieNode() {

//    next = null;
  }

  public TrieNode(HashMap<Character, TrieNode> children, int wordCount) {
    this.children = children;
    this.wordCount = wordCount;
  }

  public synchronized void incrementWordCount(int count) {
    wordCount += count;
  }

  public String toString() {
    StringBuilder childrenStr = new StringBuilder();
    for (Entry<Character, TrieNode> entry : children.entrySet()) {
      childrenStr
          .append("{" + entry.getKey().toString() + "\n, " + entry.getValue().toString() + "}");
    }
    return "\n{wordcount=" + this.wordCount + ", children: " + childrenStr + "}\n";

  }
}
public  class Trie implements Serializable{
  public  TrieNode root;
  private Integer count;

  public Trie() {
    root = new TrieNode();
    count = 0;
//    next = null;
  }
  public Trie(TrieNode trieNode){
    root = trieNode;
    count = 0;
//    next = null;
  }
  public Trie(TrieNode trieNode, int count){
    root = trieNode;
    this.count = count;
//    next = null;
  }

  private synchronized void increaseTrieCount(int diff){
    count+=diff;
  }
  public String toString(){
    return "{count: "+count+(root==null?null:", TrieNode: "+root.toString())+"}";
  }
//  // Inserts a word into the trie.
  public void insert(String word, int count) {
    TrieNode cur = root;

    Map<Character, TrieNode> curChildren = root.children;
    char[] wordArray = word.toCharArray();
//    System.out.println("insertWord: "+word+", possible position add: "+wordArray.length);
//    cur.possiblePosition.add(wordArray.length);
    for(int i = 0; i < wordArray.length; i++){
      char wc = wordArray[i];
      if(curChildren.containsKey(wc)){
        cur = curChildren.get(wc);
      } else {
        TrieNode newNode = new TrieNode();//new TrieNode(wc);
        curChildren.put(wc, newNode);
        cur = newNode;
      }
//      cur.possiblePosition.add(wordArray.length-i-1);
//      System.out.println("insertWord: "+word+", possible position add: "+(wordArray.length-i-1));
      curChildren = cur.children;
      if(i == wordArray.length - 1){
        cur.incrementWordCount(count);
        increaseTrieCount(count);
      }
    }
  }

  // Returns if the word is in the trie.
  public boolean search(String word) {
    return search(word,root);
  }
  public boolean search(String word, TrieNode node) {
    return search(word.toCharArray(),node);
  }
  public boolean search(char[] word, TrieNode node) {
    TrieNode endNode = searchWordNodePos(word,node);
    if( endNode == null){
      return false;
    } else if(endNode.wordCount > 0)
      return true;
    else return false;
  }


  public TrieNode searchWordNodePos(String s){
   return searchWordNodePos(s,root);
  }

  public TrieNode searchWordNodePos(String s, TrieNode node){
    return searchWordNodePos(s.toCharArray(),node);
  }
  public TrieNode searchWordNodePos(char[] s, TrieNode node){
    if (s.length == 0 || node==null){return node;}
    Map<Character, TrieNode> children = node.children;
    TrieNode cur = null;
    char[] sArray = s;
    for(int i = 0; i < sArray.length; i++){
      char c = sArray[i];
      if(children.containsKey(c)){
        cur = children.get(c);
        children = cur.children;
      } else{
        return null;
      }
    }
    return cur;
  }

  private void updateCandidateSet(StringBuilder result, String prevString,LanguageModel lm, PriorityQueue<Pair<String, Double>> canSet, Map<String,Integer> termToEdit, int remainingEditsAllowed){
    String newCan = result.toString();
    if (!termToEdit.containsKey(newCan)){
      termToEdit.put(newCan,Config.correctionDistance-remainingEditsAllowed);
    }else{
      int currEditDiff = termToEdit.get(newCan);
      int newEditDiff = Config.correctionDistance-remainingEditsAllowed;
      if (currEditDiff > newEditDiff){
        termToEdit.put(newCan,newEditDiff);
      }
        return;
    }
//        System.out.println("Adding: "+result.toString());
    double newCanScore = lm.bigramJointProb(prevString,newCan);
    if (canSet.size()< Config.candidateSetSize){

      canSet.add(new Pair<>(newCan,newCanScore));
    }else{
      if (newCanScore > canSet.peek().getSecond()){
        canSet.poll();
        canSet.add(new Pair<>(newCan,newCanScore));
      }
    }
  }
  public void dfsGen(char[] original, Set<Character> alternativeChars, int curr, int distance,
      StringBuilder result, PriorityQueue<Pair<String, Double>> canSet,  Map<String,Integer> termToEdit , TrieNode node,
      LanguageModel lm, String prevString){
    // TODO: remove the TEST
//    String TEST = new String(original);

    int originalLen = original.length;
    int remainingLen = originalLen-curr;
    int startingLen = result.length();
    if (distance<0){
      // early truncation
      return;
    } else if (distance == 0){
      String temp = new String(Arrays.copyOfRange(original,curr,original.length));
//      TrieNode endNode = this.searchWordNodePos(temp,node);
//      System.out.println("temp: "+ temp+", result: "+result);
//      if (temp.equals("dd")){
//        System.out.println(node);
//      }
      if (this.search(temp,node)){
        result.append(temp);
        updateCandidateSet(result,prevString, lm, canSet, termToEdit, distance);
        result.setLength(startingLen);
      }
      return;
    }
    if (curr == original.length){
      if( node != null && distance >0){
        suffixDFS(node,distance,canSet,result, prevString, lm,termToEdit,distance);
        result.setLength(startingLen);
      }
      return;
    }
    if (node == null ){
      // early truncation
      return;
    }
    Character currChar = original[curr];


    TrieNode next = null;

    // deletion
    // check if the remaining substring can existing in trie
    // we can truncate when we are so short in expect length that deletion makes no sense
    if (originalLen-(remainingLen+startingLen)<distance){
//      System.out.println("possiblePositions before deletion: ");
//      node.possiblePosition.stream().forEach(System.out::println);
//      System.out.println("deletion: "+currChar+", result: "+result.toString());
      dfsGen(original,alternativeChars,curr+1,distance-1,result,canSet,termToEdit,node, lm,prevString);
    }


    Set<Character> keySet = new HashSet<>(node.children.keySet());
    keySet.retainAll(alternativeChars);
//    if (TEST.equals("singledays")){
//      System.out.println("Before insert ");
//      System.out.println("remainingLen: "+remainingLen+", startingLen: "+startingLen +", originalLen: "+originalLen+", distance: "+distance);
//    }
    // insertion
    // we can truncate when we are so long in expected length that deletion makes no sense

    if ((remainingLen+startingLen) - originalLen < distance){
      keySet.add(' ');
      for (Character c : keySet){
        if (c != ' '){
          next = node.children.get(c);
          // check if the remaining substring can existing in trie
          result.append(c);
//          if (TEST.equals("singledays")) System.out.println("insertion: "+c+", result: "+result.toString());
          dfsGen(original,alternativeChars,curr,distance-1,result,canSet,termToEdit,next, lm,prevString);
          result.setLength(startingLen);
        }else{
//          if (TEST.equals("singledays")) System.out.println("Interstion space begin:");
          if (node != null && node.wordCount>0 && curr!=0 && curr != originalLen){

            result.append(c);
//            if (TEST.equals("singledays"))  System.out.println("insertion: <space>, result: "+result.toString());
            dfsGen(original,alternativeChars,curr,distance-1,result,canSet,termToEdit, root, lm,prevString);
            result.setLength(startingLen);
          }
//          if (TEST.equals("singledays")) System.out.println("Interstion space complete!");
        }


      }
      keySet.remove(' ');
    }
//    if (TEST.equals("singledays")){
//      System.out.println("Insert finish!");
//    }
//    // substitue
    for (Character c : keySet){
        if (!c.equals(currChar)){
          next = node.children.get(c);
          // check if the remaining substring can existing in trie
//        System.out.println("possiblePositions before substitute: ");
//        node.possiblePosition.stream().forEach(System.out::println);
//        if (node.possiblePosition.contains(remainingLen)) {
          result.append(c);
//          System.out.println("substitute: " + currChar+" to "+c + ", result: " + result.toString());
          dfsGen(original, alternativeChars, curr + 1, distance - 1, result, canSet,termToEdit,next, lm,prevString);
          result.setLength(startingLen);
//        }
        }
    }
//    // transposition
    if (curr < original.length-1){
      Character nextChar = original[curr+1];
      if (node.children.containsKey(nextChar)){
        next = node.children.get(nextChar);
        if (next.children.containsKey(currChar)){
          TrieNode nextNext = next.children.get(currChar);
//          System.out.println("possiblePositions before transposition: ");
//          node.possiblePosition.stream().forEach(System.out::println);
//          System.out.println("transposition: "+currChar+nextChar +", to: "+nextChar+currChar+", result: "+result.toString());
//          if (node.possiblePosition.contains(remainingLen)){
            result.append(nextChar);
            result.append(currChar);
            dfsGen(original,alternativeChars,curr+2,distance-1,result,canSet,termToEdit,nextNext, lm,prevString);
            result.setLength(startingLen);
//          }

        }
      }
    }
    // continue

    next = node.children.get(currChar);
    result.append(currChar);
//    System.out.println("possiblePositions before continue: ");
//    node.possiblePosition.stream().forEach(System.out::println);
//    System.out.println("continue: "+currChar+", result: "+result.toString());
    dfsGen(original,alternativeChars,curr+1,distance,result,canSet,termToEdit,next, lm, prevString);
    result.setLength(startingLen);
  }

  private void suffixDFS(TrieNode node, int distance, PriorityQueue<Pair<String, Double>> canSet,
      StringBuilder result, String prevString, LanguageModel lm, Map<String,Integer> map, int remainingBalance) {
    if (distance == 0 && node.wordCount > 0){
//      System.out.println("Adding: "+result.toString());
      updateCandidateSet(result,prevString, lm, canSet,map,remainingBalance);
    }else{
      for ( Character c: node.children.keySet()){
        int len = result.length();
        result.append(c);
        suffixDFS(node.children.get(c),distance-1,canSet,result,prevString,lm,map,remainingBalance);
        result.setLength(len);
      }
    }
  }

  public static void main(String[] args){
//    Trie test = new Trie();
//    test.insert("abdc",1);
////    String[] strings1 = {"ab","cd"};
////    List<String> array1 = Arrays.asList(strings1);
//    test.insert("bcd",0);
//
//    System.out.println(test);
//    char[] originals = "abcdde".toCharArray();
//    HashSet<Character> originalSet = new HashSet<Character>();
//    for (Character c : CandidateGenerator.alphabet){
//      originalSet.add(c);
//    }
//    Set<String> candidateSet =new HashSet<String>();
//    LanguageM
//    test.dfsGen(originals, originalSet,0,2,new StringBuilder(),candidateSet,test.root, );
//    for (String cand : candidateSet){
//      System.out.println(cand);
//    }
  }

}
