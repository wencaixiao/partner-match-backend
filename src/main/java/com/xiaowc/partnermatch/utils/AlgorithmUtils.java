package com.xiaowc.partnermatch.utils;

import java.util.List;
import java.util.Objects;

/**
 * 算法工具测试类
 */
public class AlgorithmUtils {

    /**
     * 编辑距离算法(用于计算最相似的两个字符串)：动态规划
     *    word1通过几次增删改能够变成word2
     * 比如：abc   abb     经过将c改成b即可达到两个字符串一样，所以距离是1
     *      abc   abcab   经过在abc后面加一个a和一个b即可达到两个字符串一样，所以距离是2
     * @param word1
     * @param word2
     * @return
     */
    public static int minDistance(String word1, String word2){
        int n = word1.length();
        int m = word2.length();
        if (n * m == 0) {
            return n + m;
        }
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (word1.charAt(i - 1) != word2.charAt(j - 1)) {
                    left_down += 1;
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

    /**
     * 编辑距离算法(用于计算最相似的两组标签列表)：动态规划
     *    word1通过几次增删改能够变成word2
     *  距离越小越相似
     * 比如：abc   abb     经过将c改成b即可达到两个字符串一样，所以距离是1
     *      abc   abcab   经过在abc后面加一个a和一个b即可达到两个字符串一样，所以距离是2
     * @param tagList1 第一组标签
     * @param tagList2 第二组标签
     * @return
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2){
        int n = tagList1.size();
        int m = tagList2.size();
        if (n * m == 0) {
            return n + m;
        }
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (!Objects.equals(tagList1.get(i - 1), tagList2.get(j - 1))) {
                    left_down += 1;
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }
}
