package dji.v5.ux.core.base.charts.formatter;

import dji.v5.ux.core.base.charts.model.SliceValue;

public class SimplePieChartValueFormatter implements PieChartValueFormatter {
    private ValueFormatterHelper valueFormatterHelper;

    public SimplePieChartValueFormatter() {
        this.valueFormatterHelper = new ValueFormatterHelper();
        this.valueFormatterHelper.determineDecimalSeparator();
    }

    public SimplePieChartValueFormatter(int decimalDigitsNumber) {
        this();
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
    }

    public int formatChartValue(char[] formattedValue, SliceValue value) {
        return this.valueFormatterHelper.formatFloatValueWithPrependedAndAppendedText(formattedValue, value.getValue(), value.getLabelAsChars());
    }

    public int getDecimalDigitsNumber() {
        return this.valueFormatterHelper.getDecimalDigitsNumber();
    }

    public SimplePieChartValueFormatter setDecimalDigitsNumber(int decimalDigitsNumber) {
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
        return this;
    }

    public char[] getAppendedText() {
        return this.valueFormatterHelper.getAppendedText();
    }

    public SimplePieChartValueFormatter setAppendedText(char[] appendedText) {
        this.valueFormatterHelper.setAppendedText(appendedText);
        return this;
    }

    public char[] getPrependedText() {
        return this.valueFormatterHelper.getPrependedText();
    }

    public SimplePieChartValueFormatter setPrependedText(char[] prependedText) {
        this.valueFormatterHelper.setPrependedText(prependedText);
        return this;
    }

    public char getDecimalSeparator() {
        return this.valueFormatterHelper.getDecimalSeparator();
    }

    public SimplePieChartValueFormatter setDecimalSeparator(char decimalSeparator) {
        this.valueFormatterHelper.setDecimalSeparator(decimalSeparator);
        return this;
    }
}
