package cn.com.yusys.rddduibi;

import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Int;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;


/**
 *
 *  大数据量比对 ，rdd1.subtract(rdd2) 会产生笛卡尔积，并且会产生大量的Shuffle.
 *  因此关于string的对比，可以根据string的长度进行分组排序，按分区比对。
 *  对比出增删改的数据
 */

public class Demo2 {
    public static void main(String[] args) {
        SparkConf conf = new SparkConf().setAppName("test").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);

        JavaRDD<String> rdd1 = sc.parallelize(Arrays.asList("123b","123b","123b","123c","123a","123s","1234","4567"));

        JavaRDD<String> rdd2 = sc.parallelize(Arrays.asList("123b","dd是f","asdf","sd24","fefq","d23f"));

        JavaPairRDD<Integer, Iterable<String>> pairRDD1 = rdd1.groupBy(new Function<String, Integer>() {
            @Override
            public Integer call(String s) throws Exception {
                return s.length() % 6;
            }
        });

        JavaPairRDD<Integer, Iterable<String>> pairRDD2 = rdd2.groupBy(new Function<String, Integer>() {
            @Override
            public Integer call(String s) throws Exception {
                return s.length() % 6;

            }
        });

        JavaPairRDD<Integer, Tuple2<Iterable<Iterable<String>>, Iterable<Iterable<String>>>> cogroupRdd = (JavaPairRDD<Integer, Tuple2<Iterable<Iterable<String>>, Iterable<Iterable<String>>>>) pairRDD1.cogroup(pairRDD2);

        JavaRDD<List<String>> map = cogroupRdd.map((Function<Tuple2<Integer, Tuple2<Iterable<Iterable<String>>, Iterable<Iterable<String>>>>, List<String>>) tuple2 -> {
            Iterable<Iterable<String>> iterables1 = tuple2._2._1;
            Iterable<Iterable<String>> iterables2 = tuple2._2._2;

            List<String> list1 = new ArrayList<>();
            for (Iterable<String> iterable : iterables1) {
                list1.addAll(Lists.newArrayList(iterable));
            }
            List<String> list2 = new ArrayList<>();
            for (Iterable<String> iterable : iterables2) {
                list2.addAll(Lists.newArrayList(iterable));
            }
            Collections.sort(list1);
            Collections.sort(list2);


            List<String> returnList = new ArrayList<>();

            for(int i =0;i<list1.size();i++){
                boolean flag = false;

                for(int k =0 ;k<list2.size();k++){
                    if(list1.get(i).compareTo(list2.get(k)) ==0){
                        flag = true;
                        break;
                    }
                }

                if(!flag){
                    returnList.add("insert::"+list1.get(i));
                }

            }

            int ii = returnList.size();
            System.out.println("insert size = " + ii);

            for(int i =0;i<list2.size();i++){
                boolean flag = false;
                for(int k =0 ;k<list1.size();k++){
                    if(list2.get(i).compareTo(list1.get(k)) ==0){
                        flag = true;
                        break;
                    }
                }

                if(!flag){
                    returnList.add("delete::"+list2.get(i));
                }
            }

            System.out.println("delete size = "+ (returnList.size()-ii));

            return returnList;
        });

//    map.foreachPartition((VoidFunction<Iterator<List<String>>>) listIterator -> {
//      while (listIterator.hasNext()) {
//        List<String> next = listIterator.next();
//        System.out.println(next);
//      }
//    });
        JavaRDD<String> resrdd = map.flatMap(strings ->strings.iterator());

        resrdd.foreach(s -> System.out.println(s));


    }
}
