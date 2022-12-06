package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    public static String getJVMNameFromType(String name) {
        return Environment.getType(name).getJvmName();
    }

    public static Boolean isList(String value) {
        return value.contains("PlcList");
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print ("public class Main {");
        indent++;
        newline(0); // blank line

        if (!ast.getGlobals().isEmpty()) {
            for (Ast.Global global : ast.getGlobals()) {
                newline(indent);
                print(global);
            }
            //newline(0); // blank line
        }

        newline(indent);
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        print("System.exit(new Main().main());");
        indent--;
        newline(indent);
        print("}");
        newline(0); // blank line

        for (Ast.Function function : ast.getFunctions()) {
            newline(indent);
            print(function);
            //print(function);
        }
        newline(0);

        indent--;
        newline(indent);
        print("}");

        return null;
    }


    @Override
    public Void visit(Ast.Global ast) {
        //manually print java type name
        print(ast.getVariable().getType().getJvmName());
        if (isList(ast.getValue().get().toString())) {
            print("[]");
        }
       print(" ", ast.getName());

       if (ast.getValue().isPresent()) {
           print(" = ");
           visit(ast.getValue().get());
       }

         print(";");
       return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        if (ast.getReturnTypeName().isPresent()) print(getJVMNameFromType(ast.getReturnTypeName().get()));
        print(" ", ast.getName(), "(");
        int parameterCount = ast.getParameters().size();
        int typeNamesCount = ast.getParameterTypeNames().size();
        if (parameterCount == typeNamesCount && parameterCount > 0) {
            for (int i = 0; i < parameterCount; i++) {
                print(getJVMNameFromType(ast.getParameterTypeNames().get(i)));
                print(" ", ast.getParameters().get(i));
                if (i < parameterCount - 1) print(", ");
            }
        }
        print(") {");
        //newline(0);
        if (!ast.getStatements().isEmpty()) {

            indent++;
            for (Ast.Statement statement : ast.getStatements()) {

               newline(indent);
                print(statement);

            }
            indent--;
            newline(indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if (ast.getTypeName().isPresent()) {
            print(getJVMNameFromType(ast.getTypeName().get()));
        }
        else if (ast.getValue().isPresent()) {
            print(ast.getValue().get().getType().getJvmName());
        }
        else throw new RuntimeException("Declaration must have a type or value");

        print(" ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
       print(ast.getReceiver());
       print(" = ");
       print(ast.getValue());
       print(";");
       return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        print(ast.getCondition());
        print(") {");
        indent++;

        for (Ast.Statement statement : ast.getThenStatements()) {
            newline(indent);
            print(statement);
        }

        if (!ast.getElseStatements().isEmpty()) {
            newline(indent - 1);
            print("} else {");

            for (Ast.Statement statement : ast.getElseStatements()) {
                newline(indent);
                print(statement);
            }

        }

        newline(indent - 1);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
       print ("switch (", ast.getCondition(), ") {");
       indent++;
       for (Ast.Statement.Case cas : ast.getCases()) {
           newline(indent);
           visit(cas);
       }
       indent--;
       newline(indent--);
       print("}");
       return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
      if (ast.getValue().isPresent()) {
            print("case ", ast.getValue().get(), ":");
            for (Ast.Statement statement : ast.getStatements()) {
                newline(indent + 1);
                print(statement);
            }
            newline(indent + 1);
            print("break;");
        }
        else {
            print("default:");
             for (Ast.Statement statement : ast.getStatements()) {
                newline(indent + 1);
                print(statement);
            }
      }
      return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");

        if (!ast.getStatements().isEmpty()){
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++){
                if (i != 0) {
                    newline(indent);
                }

                print(ast.getStatements().get(i));

            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        try {
            if (ast.getType() == Environment.Type.BOOLEAN) {
                Boolean loc = (Boolean) ast.getLiteral();
                print(loc);
            } else if (ast.getType() == Environment.Type.INTEGER) {
                BigInteger loc = (BigInteger) ast.getLiteral();
                print(loc);
            } else if (ast.getType() == Environment.Type.DECIMAL) {
                BigDecimal loc = (BigDecimal) ast.getLiteral();
                print(loc.toString());
            } else if (ast.getType() == Environment.Type.CHARACTER) {
                Character loc = (Character) ast.getLiteral();
                print("'", loc, "'");
            } else if (ast.getType() == Environment.Type.STRING) {
                String loc = (String) ast.getLiteral();
                print("\"", loc, "\"");
            } else if (ast.getType() == Environment.Type.NIL) {
                print("nil");
            }
            else throw new RuntimeException("Invalid type");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        writer.write("(");
        print(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        if (ast.getOperator().equals("^")){
            // print the values of left and right inside of math.pow
            print("Math.pow(");
            print(ast.getLeft());
            print(", ");
            print(ast.getRight());
            print(")");
            //print("math.pow(" + ast.getLeft() + ", " + ast.getRight() + ")");
            return null;
        }

        visit(ast.getLeft());
        switch (ast.getOperator()) {
            case "&&":
                print(" && ");
                break;
            case "||":
                print(" || ");
                break;
            case "+":
                print(" + ");
                break;
            case "-":
                print(" - ");
                break;
            case "*":
                print(" * ");
                break;
            case "/":
                print(" / ");
                break;
            case "%":
                print(" % ");
                break;
            default:
                print(" ", ast.getOperator(), " ");
                break;
        }
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()) {
            print(ast.getVariable().getValue());
            print(".");
        }

        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
       Environment.Function function = ast.getFunction();
       print(function.getJvmName(), "(");
       if (!ast.getArguments().isEmpty()) {
           for (int i = 0; i < ast.getArguments().size(); i++) {
               visit(ast.getArguments().get(i));

               if (i != ast.getArguments().size() - 1) print(", ");
           }
       }
         print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        if (!ast.getValues().isEmpty()) {
            for (int i = 0; i < ast.getValues().size(); i++) {
                visit(ast.getValues().get(i));

                if (i != ast.getValues().size() - 1) print(", ");
            }
        }
        print("}");
        return null;
    }

}
