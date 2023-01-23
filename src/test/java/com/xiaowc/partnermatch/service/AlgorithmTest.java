package com.xiaowc.partnermatch.service;

import com.xiaowc.partnermatch.utils.AlgorithmUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 算法工具类测试
 */
public class AlgorithmTest {

    /**
     * 编辑距离算法(用于计算最相似的两个字符串)：动态规划
     *    word1通过几次增删改能够变成word2
     * 比如：abc   abb     经过将c改成b即可达到两个字符串一样，所以距离是1
     *      abc   abcab   经过在abc后面加一个a和一个b即可达到两个字符串一样，所以距离是2
     */
    @Test
    void test() {
        String str1 = "xiaowcisDog";
        String str2 = "xiaowcisCat";
        String str3 = "xiaowcisDogCat";
        int score1 = AlgorithmUtils.minDistance(str1, str2); // 3
        int score2 = AlgorithmUtils.minDistance(str1, str3); // 3
        System.out.println(score1);
        System.out.println(score2);
    }

    /**
     * 编辑距离算法(用于计算最相似的两个标签列表)：动态规划
     *    word1通过几次增删改能够变成word2
     *  距离越小越相似
     * 比如：abc   abb     经过将c改成b即可达到两个字符串一样，所以距离是1
     *      abc   abcab   经过在abc后面加一个a和一个b即可达到两个字符串一样，所以距离是2
     */
    @Test
    void testCompareTags() {
        List<String> tagList1 = Arrays.asList("java", "大一", "男");
        List<String> tagList2 = Arrays.asList("java", "大二", "女");
        List<String> tagList3 = Arrays.asList("python", "大二", "女");
        int score1 = AlgorithmUtils.minDistance(tagList1, tagList2); // 2
        int score2 = AlgorithmUtils.minDistance(tagList1, tagList3); // 3
        System.out.println(score1);
        System.out.println(score2);
    }
}
