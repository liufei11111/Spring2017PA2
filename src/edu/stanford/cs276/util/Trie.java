package edu.stanford.cs276.util;

import edu.stanford.cs276.CandidateGenerator;
import edu.stanford.cs276.Config;
import java.io.ByteArrayInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

class TrieNode implements Serializable{
  // Initialize your data structure here.
//  char c;

  public Trie next;
  HashMap<Character, TrieNode> children = new HashMap<Character, TrieNode>(Config.hashMapInitialSize);// don't need that much space
  int wordCount;
//  Set<Integer> possiblePosition= new HashSet<Integer>();
  public TrieNode(){
    next = null;
  }

  public TrieNode(Trie next,HashMap<Character, TrieNode> children, int wordCount){
    this.next = next;
    this.children = children;
    this.wordCount = wordCount;
  }

  public synchronized void incrementWordCount(int count){
    wordCount+=count;
  }

  public String toString(){
    StringBuilder childrenStr = new StringBuilder();
    for (Entry<Character, TrieNode> entry: children.entrySet()){
      childrenStr.append("{"+entry.getKey().toString()+"\n, "+entry.getValue().toString()+"}");
    }
//    children.entrySet().stream().forEach((x)->{ childrenStr.append("{"+x.getKey().toString()+"\n, "+x.getValue().toString()+"}");});
    return "\n{wordcount="+this.wordCount+", children: "+childrenStr+(next==null?null:", Trie: "+next.toString())+"}\n";
  }

  public byte[] serialize() {
    int size = children.size();
    List<byte[]> listAssetBytes = new LinkedList<>();
    int childrenByteSize = 0;
    for (Entry<Character, TrieNode> entry : children.entrySet()){
      byte[] entryBytes = serializeChildEntry(entry);
      childrenByteSize+=entryBytes.length;
      listAssetBytes.add(entryBytes);
    }
    int sizeOfTrie = 0;

    if (this.next != null){
      byte[] trieBytes = this.next.serialize();
      sizeOfTrie+=trieBytes.length;
      listAssetBytes.add(trieBytes);
    }
    int totalSize = 4*4 + childrenByteSize + sizeOfTrie; // char is of size 2 in java
    ByteBuffer bb = ByteBuffer.allocate(totalSize);

    bb.putInt(size);
    bb.putInt(wordCount);
    bb.putInt(childrenByteSize);
    bb.putInt(sizeOfTrie);
//    System.out.println("Serialize TrieNode: size: "+size+", wordCount: "+wordCount
//        +", childrenByteSize: "+childrenByteSize+",sizeOfTrie "+sizeOfTrie);
    for (byte[] array : listAssetBytes){
      bb.put(array);
    }
    return bb.array();
  }



  public static synchronized TrieNode deserialize(byte[]  bytesArray, int startI, int endI){
    if (startI==endI){
      return new TrieNode();
    }
    System.out.println("TrieNode deserial: start1: "+startI+", endI: "+endI);
    int numOfMetaBytes = 4*4;
    ByteBuffer bytes = ByteBuffer.wrap(bytesArray,startI,numOfMetaBytes);
    int size = bytes.getInt();
    int wordCount = bytes.getInt();
    int childrenByteSize = bytes.getInt();
    int sizeOfTrie = bytes.getInt();
    startI += numOfMetaBytes;
    System.out.println("TrieNode deserial: start2: "+startI+", endI: "+endI);
    HashMap<Character,TrieNode> map = parseChildrenFromBytes(bytesArray, startI,startI+childrenByteSize, size);
    System.out.println("TrieNode deserial: start3: "+startI+", endI: "+endI);
    startI +=childrenByteSize;
    Trie nextTrie = null;
    System.out.println("TrieNode deserial: start4: "+startI+", endI: "+endI);
    if (sizeOfTrie !=0){
      nextTrie = Trie.deserialize(bytesArray,startI,startI+sizeOfTrie);
      startI+=sizeOfTrie;
    }

//    if (startI != endI){
//      System.out.println("StartI 5: "+startI+", EndI: "+endI);
//      throw new RuntimeException("Deserialize TrieNode mistaches!");
//    }
    return new TrieNode(nextTrie,map,wordCount);
  }
  private static HashMap<Character,TrieNode> parseChildrenFromBytes(byte[] bytes, int startI, int endI, int size) {
    if (startI==endI){
      return new HashMap<>(1);
    }
    System.out.println("parseChildrenFromBytes parse: startIe: "+startI+", endI: "+endI+", size: "+size);
    HashMap<Character, TrieNode> map = new HashMap<Character,TrieNode>(Config.hashMapInitialSize);
    for (int i=0;i<size;++i){
      char key = ByteBuffer.wrap(bytes,startI,2).getChar();
      System.out.println("Key: "+key);
      startI+=2;
      int sizeTrieNode = ByteBuffer.wrap(bytes,startI,4).getInt();
      System.out.println("sizeTrieNode: "+sizeTrieNode);
      startI+=4;
      TrieNode trieNode = TrieNode.deserialize(bytes,startI,startI+sizeTrieNode);
      startI+=sizeTrieNode;
      map.put(key,trieNode);
    }
//    if (startI != endI){
//      throw new RuntimeException("Deserialization is compromised!");
//    }
    return map;
  }
  private byte[] serializeChildEntry(Entry<Character, TrieNode> entry) {

    byte[] trieNode = entry.getValue().serialize();
    ByteBuffer bb = ByteBuffer.allocate(trieNode.length+2+4);// char is of size 2
    bb.putChar(entry.getKey().charValue());
    byte[] valueArray = entry.getValue().serialize();
    bb.putInt(valueArray.length);
    bb.put(valueArray);
//    System.out.println("SerialChildEntry total: "+trieNode.length+2+4+", entry.getKey().charValue(): "+entry.getKey().charValue()+", valueArray.length: "+valueArray.length);
    return bb.array();
  }

//  public byte[] serialize(){
//
//    byte[] next = next.serliaze();
//    int byteTotal = 0;
//    List<byte[]> entries = new LinkedList<>();
//    for(Entry<>){}
//  }
}

public class Trie implements Serializable{
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
  public static Trie deserialize(byte[] b, int startI, int endI){
    if (startI==endI){
      return null;
    }
    System.out.println("Trie deserial: start: "+startI+", endI: "+endI);
    ByteBuffer bb = ByteBuffer.wrap(b,startI,4);
    startI+=4;
    int count = bb.getInt();
    System.out.println("Count: "+count);
    TrieNode node = TrieNode.deserialize(b,startI,startI+count);
    System.out.println("node: "+node);
    startI+=count;
    System.out.println("startI end : "+startI);
//    if (startI != endI){
//      throw new RuntimeException("Deserialize Trie mismatches the meta data!");
//    }
    return new Trie(node, count);
  }

  public byte[] serialize(){

    byte[] nodeBytes = this.root.serialize();
//    System.out.println(this.root);
    int memorySize = nodeBytes.length+4;
//    System.out.println("Trie Serial: memory size: "+memorySize+", count: "+count);
    ByteBuffer bb = ByteBuffer.allocate(memorySize);
    bb.putInt(count);
    bb.put(nodeBytes);
    return bb.array();
  }
  private synchronized void increaseTrieCount(int diff){
    count+=diff;
  }
  public String toString(){
    return "{count: "+count+(root==null?null:", TrieNode: "+root.toString())+"}";
  }
//  // Inserts a word into the trie.
//  public void insert(String word) {
//    TrieNode cur = root;
//
//    HashMap<Character, TrieNode> curChildren = root.children;
//    char[] wordArray = word.toCharArray();
////    System.out.println("insertWord: "+word+", possible position add: "+wordArray.length);
////    cur.possiblePosition.add(wordArray.length);
//    for(int i = 0; i < wordArray.length; i++){
//      char wc = wordArray[i];
//      if(curChildren.containsKey(wc)){
//        cur = curChildren.get(wc);
//      } else {
//        TrieNode newNode = new TrieNode(this);//new TrieNode(wc);
//        curChildren.put(wc, newNode);
//        cur = newNode;
//      }
////      cur.possiblePosition.add(wordArray.length-i-1);
////      System.out.println("insertWord: "+word+", possible position add: "+(wordArray.length-i-1));
//      curChildren = cur.children;
//      if(i == wordArray.length - 1){
//        cur.incrementWordCount();
//        increaseTrieCount();
//      }
//    }
//  }
  public void insert(List<String> words){
    insert(words,0);
  }
  private void insert(List<String> words, int index) {
    insert(words,index,1);
  }
  private void insert(List<String> words, int index, int count) {
    if (index==words.size()){return;}
    TrieNode cur = this.root;
//      TrieNode head = cur;
    HashMap<Character, TrieNode> curChildren = cur.children;
    char[] wordArray = words.get(index).toCharArray();
//    System.out.println("insertWord: "+word+", possible position add: "+wordArray.length);
//    cur.possiblePosition.add(wordArray.length);
    for(int i = 0; i < wordArray.length; i++){
      char wc = wordArray[i];
      if(curChildren.containsKey(wc)){
        cur = curChildren.get(wc);
      } else {
        TrieNode newNode = new TrieNode();//new TrieNode(wc);
//          if (index<words.size()){
//            newNode.setParent(cur.parent);
//
//          }
        curChildren.put(wc, newNode);
        cur = newNode;
      }
//      cur.possiblePosition.add(wordArray.length-i-1);
//      System.out.println("insertWord: "+word+", possible position add: "+(wordArray.length-i-1));
      curChildren = cur.children;
      if(i == wordArray.length - 1){
        cur.incrementWordCount(count);
        this.increaseTrieCount(count);
        if (index<words.size()-1){
          if (cur.next == null){
            cur.next = new Trie();
            cur.next.insert(words,index+1);
          }else{
            cur.next.insert(words, index+1);
          }
        }

      }
    }

  }
//
//  public void insert(List<String> words, int index,  Trie parent) {
//      if (index==words.size()){return;}
//      TrieNode cur = parent.root;
////      TrieNode head = cur;
//      HashMap<Character, TrieNode> curChildren = cur.children;
//      char[] wordArray = words.get(index).toCharArray();
////    System.out.println("insertWord: "+word+", possible position add: "+wordArray.length);
////    cur.possiblePosition.add(wordArray.length);
//      for(int i = 0; i < wordArray.length; i++){
//        char wc = wordArray[i];
//        if(curChildren.containsKey(wc)){
//          cur = curChildren.get(wc);
//        } else {
//          TrieNode newNode = new TrieNode();//new TrieNode(wc);
////          if (index<words.size()){
////            newNode.setParent(cur.parent);
////
////          }
//          curChildren.put(wc, newNode);
//          cur = newNode;
//        }
////      cur.possiblePosition.add(wordArray.length-i-1);
////      System.out.println("insertWord: "+word+", possible position add: "+(wordArray.length-i-1));
//        curChildren = cur.children;
//        if(i == wordArray.length - 1){
//          cur.incrementWordCount();
//          parent.increaseTrieCount();
//          if (index<words.size()-1){
//            if (cur.next == null){
//                cur.next = new Trie();
//                cur.next.insert(words,index+1,cur.next);
//            }else{
//              cur.next.insert(words, index+1,cur.next);
//            }
//          }
//
//        }
//      }
//
//  }
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
  // Returns if there is any word in the trie
  // that starts with the given prefix.
  public boolean startsWith(String prefix) {
    if(searchWordNodePos(prefix) == null){
      return false;
    } else return true;
  }

  public TrieNode searchWordNodePos(String s){
   return searchWordNodePos(s,root);
  }

  public TrieNode searchWordNodePos(String s, TrieNode node){
    return searchWordNodePos(s.toCharArray(),node);
  }
  public TrieNode searchWordNodePos(char[] s, TrieNode node){
    if (s.length == 0){return node;}
    HashMap<Character, TrieNode> children = node.children;
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
  public TrieNode searchWordNodePos(String[] strs, int startI, int endI, Trie map){
    if (startI >= endI||map==null){return null;}
    HashMap<Character, TrieNode> children = map.root.children;
    TrieNode cur = null;
    char[] sArray = strs[startI].toCharArray();
    for(int i = 0; i < sArray.length; i++){
      char c = sArray[i];
      if(children.containsKey(c)){
        cur = children.get(c);
        children = cur.children;
      } else{
        return null;
      }
    }
    if (startI<endI-1){
      return searchWordNodePos(strs,startI+1,endI,cur.next);
    }else{
      return cur;
    }

  }
  public Double unigramProbForTerm(String word){
    TrieNode node = searchWordNodePos(word);
    if (node == null){
      return 0.0;
    }else{
      return node.wordCount*1.0/this.count;
    }
  }
  public Double getBigramProbFor(String[] terms, int startI){
    double unigramLogProd = 0.0;
    if (startI<=terms.length-1){
      TrieNode node = searchWordNodePos(terms[startI]);
      if (node !=null){
        unigramLogProd = node.wordCount*1.0/this.count;
      }
      TrieNode biNode = searchWordNodePos(terms,startI,startI+2,this);
      double bigramWordCount = 0.0;
      if (node != null){
        bigramWordCount = node.wordCount;
      }

      return Config.smoothingFactor*unigramProbForTerm(terms[startI])+(1-Config.smoothingFactor)*bigramWordCount/node.wordCount;
    }else{
      return 0.0;
    }
  }
  public void dfsGen(char[] original, Set<Character> alternativeChars,int curr, int distance, StringBuilder result, Set<String> canSet, TrieNode node){
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
//        System.out.println("Adding: "+result.toString());
        canSet.add(result.toString());
        result.setLength(startingLen);
      }
      return;
    }
    if (curr == original.length){
      if( node != null ){
        suffixDFS(node,distance,canSet,result);
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
      dfsGen(original,alternativeChars,curr+1,distance-1,result,canSet,node);
    }


    Set<Character> keySet = new HashSet<>(node.children.keySet());
    keySet.retainAll(alternativeChars);

    // insertion
    // we can truncate when we are so long in expected length that deletion makes no sense
//    System.out.println("remainingLen: "+remainingLen+", startingLen: "+startingLen +", originalLen: "+originalLen+", distance: "+distance);
    if ((remainingLen+startingLen) - originalLen < distance){
      keySet.add(' ');
      keySet.add('-');
      for (Character c : keySet){
        if (c != ' '){
          next = node.children.get(c);
          // check if the remaining substring can existing in trie
          result.append(c);
//          System.out.println("insertion: "+c+", result: "+result.toString());
          dfsGen(original,alternativeChars,curr,distance-1,result,canSet,next);
          result.setLength(startingLen);
        }else{
//          System.out.println("Interstion space begin:");
          if (node != null && node.wordCount>0 && curr!=0 && curr != originalLen&& node.next!=null){

            result.append(c);
//            System.out.println("insertion: <space>, result: "+result.toString());
            dfsGen(original,alternativeChars,curr,distance-1,result,canSet,node.next.root);
            result.setLength(startingLen);
          }
//          System.out.println("Interstion space complete!");
        }


      }
      keySet.remove(' ');
      keySet.remove('-');
    }

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
          dfsGen(original, alternativeChars, curr + 1, distance - 1, result, canSet, next);
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
            dfsGen(original,alternativeChars,curr+2,distance-1,result,canSet,nextNext);
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
    dfsGen(original,alternativeChars,curr+1,distance,result,canSet,next);
    result.setLength(startingLen);
  }

  private void suffixDFS(TrieNode node, int distance, Set<String> canSet, StringBuilder result) {
    if (distance == 0 && node.wordCount > 0){
//      System.out.println("Adding: "+result.toString());
      canSet.add(result.toString());
    }else{
      for ( Character c: node.children.keySet()){
        result.append(c);
        suffixDFS(node.children.get(c),distance-1,canSet,result);
        result.deleteCharAt(result.length()-1);
      }
    }
  }

  public static void main(String[] args){
    Trie test = new Trie();
//    test.insert("abdc");
    String[] strings1 = {"ab","cd"};
    List<String> array1 = Arrays.asList(strings1);
    test.insert(array1,0);
//    test.insert(array1,0,test);
    String[] strings2 = {"abc","ddb"};
    List<String> array2 = Arrays.asList(strings2);
    test.insert(array2,0);
//
    String[] strings3 = {"aaa"};
    List<String> array3 = Arrays.asList(strings3);
    test.insert(array3,0);
    String[] strings4 = {"cab"};
    List<String> array4 = Arrays.asList(strings4);
    test.insert(array4,0);
    String[] strings5 = {"abc"};
    List<String> array5 = Arrays.asList(strings5);
    test.insert(array5,0);

    System.out.println(test);
//    char[] originals = "abcdde".toCharArray();
//    HashSet<Character> originalSet = new HashSet<Character>();
//    for (Character c : CandidateGenerator.alphabet){
//      originalSet.add(c);
//    }
//    Set<String> candidateSet =new HashSet<String>();
//    double startingErrorRate = Math.log(0.05);
//    test.dfsGen(originals, originalSet,0,2,new StringBuilder(),candidateSet,test.root,startingErrorRate);
//    candidateSet.stream().forEach(System.out::println);
  }

//  public byte[] seralize() {
//    int sizeInteger = 4;
//    ByteBuffer bb =ByteBuffer.allocate(sizeInteger*2);
//  }
//
//  public byte[] deseralize() {
//  }
}