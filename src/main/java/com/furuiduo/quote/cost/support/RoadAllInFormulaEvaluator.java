package com.furuiduo.quote.cost.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.supplier.entity.Supplier;

/** 卡车 ALL IN 公式求值：表头别名 + 四则运算与括号。 */
public final class RoadAllInFormulaEvaluator {

  private static final List<Alias> ALIASES =
      List.of(
              new Alias("熏蒸打包价（非橡木）", "allInFmOneWay"),
              new Alias("熏蒸打包价价（非橡木）", "allInFmOneWay"),
              new Alias("ALL IN - FM (NON OAK)", "allInFmOneWay"),
              new Alias("ALL IN - FM ONE WAY", "allInFmOneWay"),
              new Alias("ALL IN - NO FM", "allInNoFm"),
              new Alias("非熏蒸打包价", "allInNoFm"),
              new Alias("TRI/TANDEM AXLE", "triTandemAxle"),
              new Alias("TRI TANDEM AXLE", "triTandemAxle"),
              new Alias("OW/TRI-AXCEL", "triTandemAxle"),
              new Alias("OW/TRI-AXLE", "triTandemAxle"),
              new Alias("OW/TRI AXLE", "triTandemAxle"),
              new Alias("OW TRI AXLE", "triTandemAxle"),
              new Alias("BASE FREIGHT", "baseFreight"),
              new Alias("WAITING FEE", "waitingFee"),
              new Alias("OTHER FEE", "otherFee"),
              new Alias("STOP OFF", "stopOff"),
              new Alias("NS LIFT", "nsLift"),
              new Alias("TO LIFT", "nsLift"),
              new Alias("CHASSIS", "chassis"),
              new Alias("REDELIVERY", "redelivery"),
              new Alias("PREPULL", "prepull"),
              new Alias("SPLIT", "split"),
              new Alias("FSC", "fsc"),
              new Alias("baseFreight", "baseFreight"),
              new Alias("fsc", "fsc"),
              new Alias("chassis", "chassis"),
              new Alias("triTandemAxle", "triTandemAxle"),
              new Alias("split", "split"),
              new Alias("stopOff", "stopOff"),
              new Alias("waitingFee", "waitingFee"),
              new Alias("redelivery", "redelivery"),
              new Alias("prepull", "prepull"),
              new Alias("nsLift", "nsLift"),
              new Alias("toLift", "nsLift"),
              new Alias("otherFee", "otherFee"),
              new Alias("allInNoFm", "allInNoFm"),
              new Alias("allInFmOneWay", "allInFmOneWay"))
          .stream()
          .sorted(Comparator.comparingInt((Alias a) -> a.alias().length()).reversed())
          .toList();

  private static final Set<String> FIELD_NAMES =
      Set.of(
          "baseFreight",
          "fsc",
          "chassis",
          "triTandemAxle",
          "split",
          "stopOff",
          "waitingFee",
          "redelivery",
          "prepull",
          "nsLift",
          "otherFee",
          "allInNoFm",
          "allInFmOneWay");

  private RoadAllInFormulaEvaluator() {}

  public static void applySupplierFormulas(CostRoad entity, Supplier supplier) {
    applySupplierFormulas(entity, supplier, false);
  }

  /**
   * @param onlyIfMissing true 时仅填充仍为空的 ALL IN，保留用户已填值
   */
  public static void applySupplierFormulas(
      CostRoad entity, Supplier supplier, boolean onlyIfMissing) {
    if (entity == null || supplier == null) {
      return;
    }
    Map<String, BigDecimal> values = feeValues(entity);
    BigDecimal noFm = evaluateNullable(supplier.getNonFumigationPackageFormula(), values);
    if (noFm != null && (!onlyIfMissing || entity.getAllInNoFm() == null)) {
      entity.setAllInNoFm(noFm);
      values.put("allInNoFm", noFm);
    } else {
      values.put("allInNoFm", nullToZero(entity.getAllInNoFm()));
    }
    BigDecimal fmOneWay =
        evaluateNullable(supplier.getFumigationNonOakPackageFormula(), values);
    if (fmOneWay != null && (!onlyIfMissing || entity.getAllInFmOneWay() == null)) {
      entity.setAllInFmOneWay(fmOneWay);
      values.put("allInFmOneWay", fmOneWay);
    } else {
      values.put("allInFmOneWay", nullToZero(entity.getAllInFmOneWay()));
    }
    BigDecimal fmRound = evaluateNullable(supplier.getFumigationOakPackageFormula(), values);
    if (fmRound != null && (!onlyIfMissing || entity.getAllInFmRound() == null)) {
      entity.setAllInFmRound(fmRound);
    }
  }

  public static BigDecimal evaluateNullable(String formula, Map<String, BigDecimal> values) {
    if (formula == null || formula.isBlank()) {
      return null;
    }
    return evaluate(formula, values);
  }

  public static BigDecimal evaluate(String formula, Map<String, BigDecimal> values) {
    List<Token> tokens = tokenize(formula);
    Parser parser = new Parser(tokens, values == null ? Map.of() : values);
    BigDecimal result = parser.parse();
    return result.setScale(2, RoundingMode.HALF_UP);
  }

  public static Map<String, BigDecimal> feeValues(CostRoad entity) {
    Map<String, BigDecimal> values = new HashMap<>();
    values.put("baseFreight", nullToZero(entity.getBaseFreight()));
    values.put("fsc", nullToZero(entity.getFsc()));
    values.put("chassis", nullToZero(entity.getChassis()));
    values.put("triTandemAxle", nullToZero(entity.getTriTandemAxle()));
    values.put("split", nullToZero(entity.getSplit()));
    values.put("stopOff", nullToZero(entity.getStopOff()));
    values.put("waitingFee", nullToZero(entity.getWaitingFee()));
    values.put("redelivery", nullToZero(entity.getRedelivery()));
    values.put("prepull", nullToZero(entity.getPrepull()));
    values.put("nsLift", nullToZero(entity.getNsLift()));
    values.put("otherFee", nullToZero(entity.getOtherFee()));
    return values;
  }

  private static BigDecimal nullToZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static AliasMatch matchFieldAt(String src, int index) {
    String slice = src.substring(index);
    for (Alias alias : ALIASES) {
      String name = alias.alias();
      if (slice.length() < name.length()) {
        continue;
      }
      if (!slice.regionMatches(true, 0, name, 0, name.length())) {
        continue;
      }
      if (slice.length() > name.length()) {
        char next = slice.charAt(name.length());
        if (Character.isLetterOrDigit(next) || next == '_') {
          if (!name.contains(" ") && !name.contains("/")) {
            continue;
          }
        }
      }
      return new AliasMatch(alias.field(), name.length());
    }
    return null;
  }

  private static List<Token> tokenize(String formula) {
    String src = formula.trim();
    List<Token> tokens = new ArrayList<>();
    int i = 0;
    while (i < src.length()) {
      char ch = src.charAt(i);
      if (Character.isWhitespace(ch)) {
        i++;
        continue;
      }
      if ("+-*/()".indexOf(ch) >= 0) {
        tokens.add(Token.opOrParen(ch));
        i++;
        continue;
      }
      if (Character.isDigit(ch) || ch == '.') {
        int j = i + 1;
        while (j < src.length()) {
          char c = src.charAt(j);
          if (!Character.isDigit(c) && c != '.') {
            break;
          }
          j++;
        }
        try {
          tokens.add(Token.number(new BigDecimal(src.substring(i, j))));
        } catch (NumberFormatException ex) {
          throw new IllegalArgumentException("无效数字: " + src.substring(i, j));
        }
        i = j;
        continue;
      }
      AliasMatch matched = matchFieldAt(src, i);
      if (matched != null) {
        if (!FIELD_NAMES.contains(matched.field())) {
          throw new IllegalArgumentException("未知字段: " + matched.field());
        }
        tokens.add(Token.field(matched.field()));
        i += matched.length();
        continue;
      }
      int j = i;
      while (j < src.length()) {
        char c = src.charAt(j);
        if (Character.isWhitespace(c) || "+-*/()".indexOf(c) >= 0) {
          break;
        }
        j++;
      }
      String unknown = src.substring(i, Math.max(j, i + 1)).trim();
      throw new IllegalArgumentException("未知字段: " + (unknown.isEmpty() ? ch : unknown));
    }
    return tokens;
  }

  private record Alias(String alias, String field) {}

  private record AliasMatch(String field, int length) {}

  private enum TokenType {
    NUMBER,
    OP,
    PAREN,
    FIELD
  }

  private record Token(TokenType type, BigDecimal number, Character symbol, String field) {
    static Token number(BigDecimal value) {
      return new Token(TokenType.NUMBER, value, null, null);
    }

    static Token opOrParen(char ch) {
      if (ch == '(' || ch == ')') {
        return new Token(TokenType.PAREN, null, ch, null);
      }
      return new Token(TokenType.OP, null, ch, null);
    }

    static Token field(String field) {
      return new Token(TokenType.FIELD, null, null, field);
    }
  }

  private static final class Parser {
    private final List<Token> tokens;
    private final Map<String, BigDecimal> values;
    private int index;

    Parser(List<Token> tokens, Map<String, BigDecimal> values) {
      this.tokens = tokens;
      this.values = values;
    }

    BigDecimal parse() {
      if (tokens.isEmpty()) {
        throw new IllegalArgumentException("公式为空");
      }
      BigDecimal result = parseExpr();
      if (index < tokens.size()) {
        throw new IllegalArgumentException("公式存在多余内容");
      }
      return result;
    }

    private Token peek() {
      return index < tokens.size() ? tokens.get(index) : null;
    }

    private Token next() {
      if (index >= tokens.size()) {
        throw new IllegalArgumentException("公式不完整");
      }
      return tokens.get(index++);
    }

    private BigDecimal parseExpr() {
      BigDecimal left = parseTerm();
      while (true) {
        Token token = peek();
        if (token == null
            || token.type() != TokenType.OP
            || (token.symbol() != '+' && token.symbol() != '-')) {
          break;
        }
        next();
        BigDecimal right = parseTerm();
        left = token.symbol() == '+' ? left.add(right) : left.subtract(right);
      }
      return left;
    }

    private BigDecimal parseTerm() {
      BigDecimal left = parseUnary();
      while (true) {
        Token token = peek();
        if (token == null
            || token.type() != TokenType.OP
            || (token.symbol() != '*' && token.symbol() != '/')) {
          break;
        }
        next();
        BigDecimal right = parseUnary();
        if (token.symbol() == '*') {
          left = left.multiply(right);
        } else {
          if (right.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("除数不能为 0");
          }
          left = left.divide(right, 8, RoundingMode.HALF_UP);
        }
      }
      return left;
    }

    private BigDecimal parseUnary() {
      Token token = peek();
      if (token != null
          && token.type() == TokenType.OP
          && (token.symbol() == '+' || token.symbol() == '-')) {
        next();
        BigDecimal value = parseUnary();
        return token.symbol() == '-' ? value.negate() : value;
      }
      return parsePrimary();
    }

    private BigDecimal parsePrimary() {
      Token token = next();
      if (token.type() == TokenType.NUMBER) {
        return token.number();
      }
      if (token.type() == TokenType.FIELD) {
        BigDecimal raw = values.getOrDefault(token.field(), BigDecimal.ZERO);
        // FSC 存百分比数值（如 35 表示 35%），参与运算时转成小数
        if ("fsc".equals(token.field())) {
          return raw.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        }
        return raw;
      }
      if (token.type() == TokenType.PAREN && token.symbol() == '(') {
        BigDecimal value = parseExpr();
        Token close = next();
        if (close.type() != TokenType.PAREN || close.symbol() != ')') {
          throw new IllegalArgumentException("缺少右括号");
        }
        return value;
      }
      throw new IllegalArgumentException("公式语法错误");
    }
  }
}
