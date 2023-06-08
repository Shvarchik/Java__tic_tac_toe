package sample;

import java.util.*;
import java.util.regex.*;

public class Program {

    private static final int WIN_COUNT = 4;
    private static final char DOT_EMPTY = '*';
    private static final char WIN_O = 'Ø';
    private static final char WIN_X = '╳';
    private static char dot_Human;
    private static char dot_AI;
    private static char[][] field;
    private static List<int[]> possibleMoves; // список возможных ходов, каждый массив - координаты точки и ее вес
    private static ArrayList<Pattern> patternX = new ArrayList<>(); // список шаблонов для комбинаций 0
    private static ArrayList<Pattern> patternO = new ArrayList<>(); // список шаблонов для комбинаций Х
    private static int[] patternWeight = new int[7]; // массив весов шаблонов
    private static int fieldSizeX; // размерность игрового поля - количество строк
    private static int fieldSizeY; // размерность игрового поля - количество столбцов
    private static int stepCounter;
    private static int maxStep;
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Random random = new Random();

    /**
     * инициализация игрового поля
     */
    private static void initialize_field() {

        field = new char[fieldSizeX][fieldSizeY];
        for (int i = 0; i < fieldSizeX; i++) {
            for (int j = 0; j < fieldSizeY; j++) {
                field[i][j] = DOT_EMPTY;
            }
        }
    }

    /**
     * Отрисовка игрового поля
     */
    private static void printField() {

        System.out.print("--|");
        for (int i = 0; i < fieldSizeY; i++) {
            System.out.printf((i + 1 < 10) ? " " + (i + 1) + " |" : " " + (i + 1) + "|");
        }
        System.out.println();
        for (int x = 0; x < fieldSizeX; x++) {
            System.out.print((x + 1 < 10) ? (x + 1) + " |" : (x + 1) + "|");

            for (int y = 0; y < fieldSizeY; y++) {
                System.out.print(" " + field[x][y] + " |");
            }
            System.out.println();
        }
        for (int i = 0; i < fieldSizeY * 4 + 3; i++) {
            System.out.print("-");
        }
        System.out.println();
    }

    /**
     * Инициализация списка шаблонов для Х и 0 в порядке убывания важности
     */
    private static void initializePatterns() {

        Pattern pat = Pattern.compile("\\*XXX\\*"); // открытая тройка - 1 ход до однозначной победы,вес 10000
        patternX.add(pat);
        pat = Pattern.compile("\\*000\\*");
        patternO.add(pat);
        patternWeight[0] = 10000;
        pat = Pattern.compile("XXX\\*"); // полуоткрытая тройка - срочно закрывать, вес 1000
        patternX.add(pat);
        pat = Pattern.compile("000\\*");
        patternO.add(pat);
        patternWeight[1] = 1000; // 500;
        pat = Pattern.compile("X\\*XX"); // тройка с брешью посередине - срочно закрывать, вес 1000
        patternX.add(pat);
        pat = Pattern.compile("0\\*00");
        patternO.add(pat);
        patternWeight[2] = 1000; // 500;
        pat = Pattern.compile("\\*X\\*X"); // двойка *X*X - вес 200 для точки посередине, 100 для крайней
        patternX.add(pat);
        pat = Pattern.compile("\\*0\\*0");
        patternO.add(pat);
        patternWeight[3] = 100;
        pat = Pattern.compile("\\*XX\\*"); // открытая двойка *ХХ* - вес 100 для обеих точек
        patternX.add(pat);
        pat = Pattern.compile("\\*00\\*");
        patternO.add(pat);
        patternWeight[4] = 100;
        pat = Pattern.compile("X\\*\\*X"); // двойка Х**Х - вес 80
        patternX.add(pat);
        pat = Pattern.compile("0\\*\\*0");
        patternO.add(pat);
        patternWeight[5] = 80;
        pat = Pattern.compile("\\*\\*XX"); // полуткрытая двойка **ХХ - вес 50
        patternX.add(pat);
        pat = Pattern.compile("\\*\\*00");
        patternO.add(pat);
        patternWeight[6] = 50;

    }

    /**
     * проверка валидности ячейки
     *
     * @param x номер строки
     * @param y номер столбца
     */
    private static boolean isCellValid(int x, int y) {

        return (x >= 0 && x < fieldSizeX && y >= 0 && y < fieldSizeY);
    }

    /**
     * проверка свободы ячейки
     * возвращает true если ячейка занята
     * 
     * @param x номер строки
     * @param y номер столбца
     */
    private static boolean isCellNotEmpty(int x, int y) {

        return field[x][y] != DOT_EMPTY;
    }

    /**
     * ход игрока
     *
     * @return координаты сделанного хода
     */
    private static int[] humanStep() {
        int y, x;
        do {
            System.out.println("Введите координаты шага (номер строки и столбца через пробел:)");
            x = SCANNER.nextInt() - 1;
            y = SCANNER.nextInt() - 1;
        } while (!isCellValid(x, y) || isCellNotEmpty(x, y));
        field[x][y] = dot_Human;
        stepCounter++;
        removeCoordinatesFromPossibleMoves(x, y);
        return new int[] { x, y };
    }

    /**
     * удаление точки с заданными координатами из массива возможных ходов AI
     *
     * @param x номер строки
     * @param y номер столбца
     */
    private static void removeCoordinatesFromPossibleMoves(int x, int y) {
        if (!possibleMoves.isEmpty()) {
            for (int[] cell : possibleMoves) {
                if (cell[0] == x && cell[1] == y) {
                    possibleMoves.remove(cell);
                    return;
                }
            }
        }
    }

    /**
     * Добавление точки с заданными координатами в массив возможных ходов AI
     * Если точка уже в массиве - увеличение ее веса на заданный
     *
     * @param x
     * @param y
     * @param weight
     */
    private static void addCoordinatesToPossibleMoves(int x, int y, int weight) {

        if (field[x][y] == DOT_EMPTY) {
            for (int[] move : possibleMoves) {
                if (x == move[0] && y == move[1]) {
                    move[2] = move[2] + weight;
                    return;
                }
            }
            possibleMoves.add(new int[] { x, y, weight });
        }
    }

    /**
     * ход компьютера
     *
     * @return координаты сделанного хода
     */
    private static int[] aiStep() {
        int[] move;
        int x, y;

        do {
            if (!possibleMoves.isEmpty()) {
                move = choiceMove();
                x = move[0];
                y = move[1];
            } else {
                x = random.nextInt(fieldSizeX);
                y = random.nextInt(fieldSizeY);
                move = new int[] { x, y };
            }

        } while (isCellNotEmpty(x, y));
        field[x][y] = dot_AI;
        stepCounter++;
        int dist = WIN_COUNT - 2;

        // удаление точки, в которую сделан ход из массива возожных ходов AI

        removeCoordinatesFromPossibleMoves(x, y);

        // все точки, отстоящие от точки хода на 1 или 2 клетки помещаются в массив
        // возможных ходов с начальным весом 10 (или их вес увеличивается на 10)

        for (int direction = 1; direction <= 4; direction++) {
            ArrayList<int[]> line = assemblyLine(x, y, dist, direction);
            for (int[] coordinates : line) {
                if (field[coordinates[0]][coordinates[1]] == '*') {
                    addCoordinatesToPossibleMoves(coordinates[0], coordinates[1], 10);
                }
            }
        }
        return move;
    }

    /**
     * Выбор хода AI по максимальному весу
     *
     * @return массив координат точки хода
     */
    private static int[] choiceMove() {
        int maxWeight = 0;
        int maxIndex = 0;
        for (int i = 0; i < possibleMoves.size(); i++) {
            if (possibleMoves.get(i)[2] > maxWeight) {
                maxWeight = possibleMoves.get(i)[2];
                maxIndex = i;
            }
        }
        return new int[] { possibleMoves.get(maxIndex)[0], possibleMoves.get(maxIndex)[1] };
    }

    /**
     * Проверка состояния игры
     *
     * @param cell - массив координат последнего сделанного хода
     */
    private static boolean gameCheck(int[] cell) {
        int cellX = cell[0];
        int cellY = cell[1];
        char checkDot = field[cellX][cellY]; // символ игрока, сделавшего последний ход
        String str;
        if (checkWinLine(cellX, cellY, 1) || checkWinLine(cellX, cellY, 2) ||
                checkWinLine(cellX, cellY, 3) || checkWinLine(cellX, cellY, 4)) {
            if (checkDot == dot_Human)
                str = "Вы победили!!!";
            else
                str = "Победил компьютер((";
            System.out.println(str);
            return true; // победа игрока, сделавшего последний ход
        }
        if (stepCounter == maxStep) {
            System.out.println("Ничья");
            return true;
        }
        return false; // играем дальше
    }

    /**
     * проверка выигрышной комбинации на одном направлении
     *
     * @param cellX     строка исходной точки (последнего сделанного хода)
     * @param cellY     столбец исходной точки
     * @param direction направление проверки:
     *                  1 - горизонталь
     *                  2 - вертикаль
     *                  3 - диагональ Л верх - П низ
     *                  4 - диагонал Л низ - П верх
     */
    static boolean checkWinLine(int cellX, int cellY, int direction) {

        ArrayList<int[]> winLine = new ArrayList<>();
        int counter = 0;
        char c = field[cellX][cellY];
        ArrayList<int[]> line = assemblyLine(cellX, cellY, WIN_COUNT - 1, direction);
        for (int[] cell : line) {
            if (field[cell[0]][cell[1]] == c) {
                winLine.add(cell);
                counter++;
                if (counter == WIN_COUNT) {
                    for (int[] coordinates : winLine) { // заменяем символы победной комбинации для красоты
                        if (c == 'X')
                            field[coordinates[0]][coordinates[1]] = WIN_X;
                        else
                            field[coordinates[0]][coordinates[1]] = WIN_O;
                    }
                    return true;
                }
            } else {
                counter = 0;
                winLine.clear();
            }
        }
        /*
         * вызов метода, добавляющего нужные точки
         * собранной линии в массив возможных
         * ходов AI
         */
        // if (c == dot_Human) {
        checkAndAddPossibleMoves(c, line);
        // }
        return false;
    }

    /**
     * Метод получает список координат линии, проверяет совпадение
     * последовательности символов линии с шаблоном и добавляет
     * необходимые точки в список возможных ходов с соответствующим
     * шаблону весом (или повышает вес, если точка уже есть в списке)
     * 
     * TODO попробовать увеличить все значения весов на 10% для атаки (если dot ==
     * dot_AI)
     * 
     * @param dot  ключевой символ шаблона
     * @param line список координат линии
     */
    static void checkAndAddPossibleMoves(char dot, ArrayList<int[]> line) {

        ArrayList<Pattern> patternArray;
        StringBuilder charLine = new StringBuilder();
        for (int[] cell : line) {
            charLine.append(field[cell[0]][cell[1]]);
        }
        // StringBuilder reverseLine = charLine.reverse();
        if (dot == 'X') {
            patternArray = new ArrayList<>(patternX);
        } else {
            patternArray = new ArrayList<>(patternO);
        }
        for (int count = 1; count <= 2; charLine = charLine.reverse(), count++) {

            for (int i = 0; i < patternArray.size(); i++) {// i - номер шаблона (и его веса)

                if (count == 1 || (i == 1 || i == 2 || i == 3 || i == 6)) { // реверс строки нужен для шаблонов 1,2,3,6

                    Pattern pat = patternArray.get(i);
                    Matcher mat = pat.matcher(charLine);
                    if (mat.find()) {
                        int index = mat.start();
                        switch (i) {
                            case 0:
                                addCoordinatesToPossibleMoves(line.get(index)[0], line.get(index)[1], patternWeight[i]);
                                addCoordinatesToPossibleMoves(line.get(index + 4)[0], line.get(index + 4)[1],
                                        patternWeight[i]);
                                break;
                            case 1:
                                if (count == 1) {
                                    addCoordinatesToPossibleMoves(line.get(index + 3)[0], line.get(index + 3)[1],
                                            patternWeight[i]);
                                } else {
                                    addCoordinatesToPossibleMoves(line.get(index)[0], line.get(index)[1],
                                            patternWeight[i]);
                                }
                                break;
                            case 2:
                                if (count == 1) {
                                    addCoordinatesToPossibleMoves(line.get(index + 1)[0], line.get(index + 1)[1],
                                            patternWeight[i]);
                                } else {
                                    addCoordinatesToPossibleMoves(line.get(index + 2)[0], line.get(index + 2)[1],
                                            patternWeight[i]);
                                }
                                break;
                            case 3:
                                if (count == 1) {
                                    addCoordinatesToPossibleMoves(line.get(index)[0], line.get(index)[1],
                                            patternWeight[i]);
                                    addCoordinatesToPossibleMoves(line.get(index + 2)[0], line.get(index + 2)[1],
                                            patternWeight[i] * 2);
                                } else {
                                    addCoordinatesToPossibleMoves(line.get(index + 1)[0], line.get(index + 1)[1],
                                            patternWeight[i] * 2);
                                    addCoordinatesToPossibleMoves(line.get(index + 3)[0], line.get(index + 3)[1],
                                            patternWeight[i]);
                                }
                                break;

                            case 4:
                                addCoordinatesToPossibleMoves(line.get(index)[0], line.get(index)[1], patternWeight[i]);
                                addCoordinatesToPossibleMoves(line.get(index + 3)[0], line.get(index + 3)[1],
                                        patternWeight[i]);
                                break;
                            case 5:
                                addCoordinatesToPossibleMoves(line.get(index + 1)[0], line.get(index + 1)[1],
                                        patternWeight[i]);
                                addCoordinatesToPossibleMoves(line.get(index + 2)[0], line.get(index + 2)[1],
                                        patternWeight[i]);
                                break;
                            case 6:
                                if (count == 1) {
                                    addCoordinatesToPossibleMoves(line.get(index + 1)[0], line.get(index + 1)[1],
                                            patternWeight[i]);
                                } else {
                                    addCoordinatesToPossibleMoves(line.get(index + 2)[0], line.get(index + 2)[1],
                                            patternWeight[i]);
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + i);
                        }
                    }
                }
            }
        }
    }

    /**
     * метод сборки линии вокруг точки
     *
     * @param cellX     координата Х (номер строки)
     * @param cellY     координата Y (номер столбца)
     * @param dist      максимальная дистанция от точки (при дистанции 3 собирается
     *                  линия из максимум 7 точек)
     * @param direction направление
     *                  1 - горизонталь
     *                  2 - вертикаль
     *                  3 - диагональ Л верх - П низ
     *                  4 - диагонал Л низ - П верх
     * @return список массивов координат точек собранной линии
     */

    private static ArrayList<int[]> assemblyLine(int cellX, int cellY, int dist, int direction) {

        int x = 0, y = 0;
        ArrayList<int[]> line = new ArrayList<>();
        for (int step = -dist; step <= dist; step++) {
            switch (direction) {
                case 1:
                    x = cellX;
                    y = cellY + step;
                    break;
                case 2:
                    x = cellX + step;
                    y = cellY;
                    break;
                case 3:
                    x = cellX + step;
                    y = cellY + step;
                    break;
                case 4:
                    x = cellX - step;
                    y = cellY + step;
                    break;
            }
            if (isCellValid(x, y)) {
                line.add(new int[] { x, y });
            }
        }
        return line;
    }

    public static void main(String[] args) {

        boolean humanFirst;
        int[] step;
        System.out.println("Введите количество строк поля:");
        fieldSizeX = SCANNER.nextInt();
        System.out.println("Введите количество столбцов поля:");
        fieldSizeY = SCANNER.nextInt();
        maxStep = fieldSizeX * fieldSizeY;
        do {
            System.out.println("Введите за кого будете играть X или 0:");

            dot_Human = SCANNER.next().charAt(0);
            possibleMoves = new ArrayList<>();
            stepCounter = 0;
            if (dot_Human == 'X') {
                humanFirst = true;
                dot_AI = '0';
            } else {
                dot_AI = 'X';
                humanFirst = false;
                possibleMoves.add(new int[] { fieldSizeX / 2, fieldSizeY / 2, 1 });
            }
            initialize_field();
            initializePatterns();
            printField();
            while (true) {
                if (humanFirst) {
                    step = humanStep();
                    humanFirst = false;
                } else {
                    step = aiStep();
                    humanFirst = true;
                    printField();
                }
                if (gameCheck(step)) {
                    printField();
                    break;
                }
            }
            System.out.println("Желаете сыграть еще раз? (Y - да)");
            if (!SCANNER.next().equalsIgnoreCase("Y"))
                break;
            else {
                patternX.clear();
                patternO.clear();
            }
        } while (true);
    }
}
