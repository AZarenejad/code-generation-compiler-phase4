package main.visitor.CodeGenerator;

import main.ast.node.Main;
import main.ast.node.Program;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.declaration.ActorInstantiation;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.operators.UnaryOperator;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
import main.ast.type.Type;
import main.ast.type.actorType.ActorType;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.noType.NoType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.symbolTable.*;
import main.symbolTable.itemException.ItemNotFoundException;
import main.symbolTable.symbolTableVariableItem.*;
import main.visitor.Translator;
import main.visitor.VisitorImpl;

import java.util.*;

public class CodeGenerator extends VisitorImpl {
    private SymbolTableActorItem curr_actor;
    private SymbolTableHandlerItem curr_handler;
    private SymbolTableMainItem curr_main;


    Translator code_generation_translator;
    String curr_actor_name;
    String curr_msg_handler_name;
    boolean in_main = false;
    boolean in_actor = false;
    boolean in_known_actor = false;
    boolean in_actor_vars = false;
    boolean in_for = false;
    boolean in_initial = false;
    boolean in_msg_handler = false;
    boolean in_condition = false;
    boolean in_assign = false;
    boolean is_left_val = false;
    boolean in_var_dec = false;
    boolean in_arr_call = false;
    boolean in_handler_call = false;
    boolean actor_id = false;
    boolean actor_var_access = false;
    boolean should_visit_init_args_in_main = false;
    private int labelGenerator = 0;
    boolean post_pre = false;
    int firstLoopLabel;
    int exitLoopLabel;



    boolean actor_name_visit_identifier = false;
    int index;
    int unique_lable_number = 0;

    private boolean msg_handler_id = false;
    private boolean msg_handler_id_call = false;
    private boolean act_var_id = false;
    private Identifier curr_id = null;
    Hashtable<String, Integer> map_actor_inst_in_main = new Hashtable<String, Integer>();
    Hashtable<String, Integer> map_var_in_handler;
    Hashtable<String, Integer> map_var_in_initial;
    Hashtable<String, String> known_actors;
    Hashtable<String, Boolean> Actor_type_has_initial = new Hashtable<String, Boolean>();
    ActorDeclaration curr_act_dec;


    private Type type_getter_from_main(Identifier id) throws ItemNotFoundException {
        Type the_type = null;
        try {
            SymbolTableVariableItem init_item = (SymbolTableVariableItem) this.curr_main.getMainSymbolTable().get(
                    SymbolTableVariableItem.STARTKEY + id.getName());
            the_type = init_item.getVarDeclaration().getType();
        } catch (ItemNotFoundException e) {
            throw new ItemNotFoundException();
        }
        return the_type;
    }

    private Type type_getter_from_actor_items(Identifier id) throws ItemNotFoundException {
        Type the_type = null;
        try {
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.get(
                    SymbolTableActorItem.STARTKEY + id.getName());

            if (actorItem.getActorDeclaration().getName().getType() instanceof NoType)
                the_type = new NoType();
            else
                the_type = new ActorType(id);
        } catch (ItemNotFoundException e) {
            throw new ItemNotFoundException();
        }
        return the_type;
    }

    private Type type_getter_from_whole_actor(Identifier id) throws ItemNotFoundException {
        Type the_type = null;
        try {
            the_type = this.type_getter_from_handler(id);
        } catch (ItemNotFoundException e) {
            try {
                the_type = this.type_getter_from_act(id);
            } catch (ItemNotFoundException e1) {
                throw new ItemNotFoundException();
            }
        }
        return the_type;
    }

    private Type type_getter_from_handler(Identifier id) throws ItemNotFoundException {
        Type the_type = null;
        try {
            SymbolTableVariableItem init_item = (SymbolTableVariableItem) this.curr_handler.getHandlerSymbolTable().get(
                    SymbolTableVariableItem.STARTKEY + id.getName());
            the_type = init_item.getVarDeclaration().getType();
        } catch (Exception e) {
            throw new ItemNotFoundException();
        }
        return the_type;
    }

    private Type type_getter_from_act(Identifier id) throws ItemNotFoundException {
        Type the_type = null;
        try {
            SymbolTableVariableItem init_item = (SymbolTableVariableItem) this.curr_actor.getActorSymbolTable().get(
                    SymbolTableVariableItem.STARTKEY + id.getName());
            the_type = init_item.getVarDeclaration().getType();
        } catch (ItemNotFoundException e) {
            throw new ItemNotFoundException();
        }
        return the_type;
    }

    private ArrayList<VarDeclaration> get_known_actors_from_actor(Identifier actor) {
        ArrayList<VarDeclaration> the_known_actors = null;
        try {
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.get(
                    SymbolTableActorItem.STARTKEY + actor.getName()
            );
            the_known_actors = actorItem.getActorDeclaration().getKnownActors();
        } catch (ItemNotFoundException e) {
            System.out.printf("There is no such %s", actor.getName());
        }
        return the_known_actors;
    }

    private Type type_getter(Identifier id) {
        Type the_type = null;
        try {
            the_type = this.type_getter_from_whole_actor(id);
        } catch (ItemNotFoundException e) {
            try {
                the_type = this.type_getter_from_main(id);
            } catch (ItemNotFoundException e1) {
                try {
                    the_type = this.type_getter_from_actor_items(id);
                } catch (ItemNotFoundException e2) {
                    the_type = new NoType();
                }
            }
        }
        return the_type;
    }

    private boolean check_first_actor_subtype_second(String act1, String ac2) {
        boolean ret = false;
        if (act1.equals("notype")) {
            ret = true;
            return ret;
        }
        SymbolTableActorItem act1_sym = null;
        try {
            act1_sym = ((SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY +
                    act1));
            SymbolTableActorItem act2_sym = ((SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ac2));

            if (act1_sym.getActorDeclaration().getName().equals(act2_sym.getActorDeclaration().getName())) {
                ret = true;
                return ret;
            }
            while (act1_sym.getParentName() != null) {
                if (act1_sym.getParentName().equals(act2_sym.getActorSymbolTable().getName())) {
                    ret = true;
                    break;
                } else {
                    act1_sym = ((SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY +
                            act1_sym.getParentName()));
                }
            }
        } catch (ItemNotFoundException e) {
            ret = false;
            return ret;
        }
        return ret;
    }


    private boolean isLeftValue(Expression expression) {
        if (expression.toString().equals("Sender")) {
            return false;
        }
        return (expression instanceof ArrayCall || expression instanceof Identifier ||
                expression instanceof ActorVarAccess);
    }


    @Override
    public void visit(Program program) {
        code_generation_translator = new Translator();
        code_generation_translator.add_default_actor_in_jasmin_folder();
        for (ActorDeclaration actorDeclaration : program.getActors()) {
            actorDeclaration.accept(this);
        }
        program.getMain().accept(this);
        code_generation_translator.printTheCommands();
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        in_actor = true;
        curr_act_dec = actorDeclaration;
        curr_actor_name = actorDeclaration.getName().getName();
        try {
            this.curr_actor = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + curr_actor_name);
        } catch (ItemNotFoundException ignored) {
        }
        actor_name_visit_identifier = true;
        visitExpr(actorDeclaration.getName());
        actor_name_visit_identifier = false;
        visitExpr(actorDeclaration.getParentName());
        code_generation_translator.create_file_for_actor(curr_actor_name, actorDeclaration);

        known_actors = new Hashtable<String, String>();
        for (VarDeclaration varDeclaration : actorDeclaration.getKnownActors()) {
            in_known_actor = true;
            varDeclaration.accept(this);
        }
        in_known_actor = false;


        for (VarDeclaration varDeclaration : actorDeclaration.getActorVars()) {
            in_actor_vars = true;
            varDeclaration.accept(this);
        }
        in_actor_vars = false;

        this.code_generation_translator.create_constructor_in_actor_file(curr_actor_name, actorDeclaration.getActorVars());

        if (actorDeclaration.getInitHandler() != null) {
            in_initial = true;
            Actor_type_has_initial.put(actorDeclaration.getName().getName(),true);
            actorDeclaration.getInitHandler().accept(this);
        }

        in_initial = false;

        this.code_generation_translator.add_set_known_actors_method_in_actor_file(curr_actor_name,
                actorDeclaration.getKnownActors());


        for (MsgHandlerDeclaration msgHandlerDeclaration : actorDeclaration.getMsgHandlers()) {
            in_msg_handler = true;
            msgHandlerDeclaration.accept(this);
        }
        in_msg_handler = false;
    }


    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        if (handlerDeclaration == null)
            return;
        msg_handler_id = true;
        visitExpr(handlerDeclaration.getName());
        msg_handler_id = false;
        curr_msg_handler_name = handlerDeclaration.getName().getName();

        try {
            this.curr_handler = (SymbolTableHandlerItem) this.curr_actor.getActorSymbolTable().get(SymbolTableHandlerItem.STARTKEY +
                    curr_msg_handler_name);
        } catch (ItemNotFoundException ignored) {
        }

        int index;
        map_var_in_handler = new Hashtable<String, Integer>();
        map_var_in_initial = new Hashtable<String, Integer>();
        if (in_initial) {
            index = 1;
            for (VarDeclaration argDeclaration : handlerDeclaration.getArgs()) {
                argDeclaration.accept(this);
                map_var_in_initial.put(argDeclaration.getIdentifier().getName(), index);
                index += 1;
            }

            for (VarDeclaration localVariable : handlerDeclaration.getLocalVars()) {
                localVariable.accept(this);
                map_var_in_initial.put(localVariable.getIdentifier().getName(), index);
                index += 1;
            }
        } else {
            index = 2;

            for (VarDeclaration argDeclaration : handlerDeclaration.getArgs()) {
                argDeclaration.accept(this);
                map_var_in_handler.put(argDeclaration.getIdentifier().getName(), index);
                index += 1;
            }

            for (VarDeclaration localVariable : handlerDeclaration.getLocalVars()) {
                localVariable.accept(this);
                map_var_in_handler.put(localVariable.getIdentifier().getName(), index);
                index += 1;
            }
        }

        if (in_initial) {
            code_generation_translator.create_msg_handler_in_actor_file(curr_actor_name, curr_msg_handler_name,
                    map_var_in_initial.size(), handlerDeclaration.getArgs(), true);
        } else {
            code_generation_translator.create_msg_handler_in_actor_file(curr_actor_name, curr_msg_handler_name,
                    map_var_in_handler.size(), handlerDeclaration.getArgs(), false);
        }

        for (VarDeclaration argDeclaration : handlerDeclaration.getArgs()) {
            if (argDeclaration.getIdentifier().getType() instanceof ArrayType) {
                int size = ((ArrayType) argDeclaration.getIdentifier().getType()).getSize();
                code_generation_translator.add_command(curr_actor_name, "ldc " + Integer.toString(size));
                code_generation_translator.add_command(curr_actor_name, "newarray int");
                make_load_identifier_code(curr_actor_name, argDeclaration.getIdentifier());
            }
        }

        for (VarDeclaration localVariable : handlerDeclaration.getLocalVars()) {
            if (localVariable.getIdentifier().getType() instanceof ArrayType) {
                int size = ((ArrayType) localVariable.getIdentifier().getType()).getSize();
                code_generation_translator.add_command(curr_actor_name, "ldc " + Integer.toString(size));
                code_generation_translator.add_command(curr_actor_name, "newarray int");
                make_load_identifier_code(curr_actor_name, localVariable.getIdentifier());
            }
        }


//        Iterator it = map_var_in_handler.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry pair = (Map.Entry)it.next();
//            System.out.println(pair.getKey() + " = " + pair.getValue());
//            it.remove(); // avoids a ConcurrentModificationException
//        }

        for (Statement statement : handlerDeclaration.getBody()) {
            visitStatement(statement);
        }
        code_generation_translator.end_msg_handler(curr_actor_name);

        if (in_msg_handler) {
            code_generation_translator.create_actor_msg_handler_seperate_file(curr_actor_name, curr_msg_handler_name,
                    handlerDeclaration.getArgs());
            code_generation_translator.make_send_of_msg_handler_in_actor_file(curr_actor_name, curr_msg_handler_name,
                    handlerDeclaration.getArgs());
            code_generation_translator.add_send_type_of_msg_handler_in_default_actor(curr_actor_name,
                    curr_msg_handler_name, handlerDeclaration.getArgs());
        }

    }


    @Override
    public void visit(VarDeclaration varDeclaration) {
        in_var_dec = true;
        visitExpr(varDeclaration.getIdentifier());
        if (in_actor && in_known_actor) {
            this.code_generation_translator.add_known_actor_to_field_of_actor_in_file(curr_actor_name, varDeclaration);
            known_actors.put(varDeclaration.getIdentifier().getName() ,varDeclaration.getIdentifier().getType().toString());
        } else if (in_actor && in_actor_vars) {
            this.code_generation_translator.add_actor_var_to_filed_of_actor_in_file(curr_actor_name, varDeclaration);
        }
        in_var_dec = false;
    }

    @Override
    public void visit(Main programMain) {
        if (programMain == null)
            return;
        in_main = true;
        try {
            this.curr_main = (SymbolTableMainItem) SymbolTable.root.get(SymbolTableMainItem.STARTKEY + "main");
            this.in_main = true;
        } catch (ItemNotFoundException ignored) {
        }
        code_generation_translator.create_main_file();

        int i = 1;
        for (ActorInstantiation mainActor : programMain.getMainActors()) {
            mainActor.accept(this);
            map_actor_inst_in_main.put(mainActor.getIdentifier().getName(), i);
            i += 1;
        }
        for (ActorInstantiation mainActor‌ : programMain.getMainActors()) {
            code_generation_translator.make_actor_instantiation_in_main(mainActor‌.getIdentifier().getType().toString(),
                    map_actor_inst_in_main.get(mainActor‌.getIdentifier().getName()));
        }

        for (ActorInstantiation mainActor‌ : programMain.getMainActors()) {
            int actor_call_index = map_actor_inst_in_main.get(mainActor‌.getIdentifier().getName());
            code_generation_translator.make_calling_set_known_actor_in_main(actor_call_index,
                    mainActor‌.getKnownActors(),
                    map_actor_inst_in_main, mainActor‌.getIdentifier());
        }

        should_visit_init_args_in_main = true;
        for (ActorInstantiation mainActor‌ : programMain.getMainActors()) {
            int actor_call_index = map_actor_inst_in_main.get(mainActor‌.getIdentifier().getName());
            if (Actor_type_has_initial.get(mainActor‌.getIdentifier().getType().toString())!=null) {
                if (actor_call_index <= 3) {
                    code_generation_translator.add_command("Main", "aload_" + Integer.toString(actor_call_index));
                } else {
                    code_generation_translator.add_command("Main", "aload " + Integer.toString(actor_call_index));
                }
                mainActor‌.accept(this);
                String cmd = "invokevirtual " + mainActor‌.getIdentifier().getType().toString() + "/initial(";
                for (int k = 0; k < mainActor‌.getInitArgs().size(); k++) {
                    cmd += code_generation_translator.get_type_code_generation_equivalent(mainActor‌.getInitArgs().get(k).getType());
                }
                cmd += ")V";
                code_generation_translator.add_command("Main", cmd);
            }
        }






        for (ActorInstantiation mainActor‌ : programMain.getMainActors()) {
            code_generation_translator.generate_code_start_for_actor_in_main(mainActor‌.getIdentifier(),
                    map_actor_inst_in_main.get(mainActor‌.getIdentifier().getName()));
        }

        code_generation_translator.end_main();
        in_main = false;
    }


    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        if (actorInstantiation == null)
            return;
        if(!should_visit_init_args_in_main) {
            visitExpr(actorInstantiation.getIdentifier());
            for (Identifier knownActor : actorInstantiation.getKnownActors()) {
                visitExpr(knownActor);
            }
        }

        if(should_visit_init_args_in_main) {
            for (Expression initArg : actorInstantiation.getInitArgs()) {
                visitExpr(initArg);
            }
        }


//
//
//        int known_pass = 0;
//        for (Identifier knownActor : actorInstantiation.getKnownActors()) {
//            visitExpr(knownActor);
//            known_pass += 1;
//        }
//        int arg_pass = 0;
//        for (Expression initArg : actorInstantiation.getInitArgs()) {
//            visitExpr(initArg);
//            arg_pass += 1;
//        }
//
//
//        SymbolTableActorItem act_sym = null;
//        ArrayList<VarDeclaration> known_act = null;
//        ArrayList<VarDeclaration> init_args = null;
//        int actial_init_arg_size = 0;
//        try {
//            act_sym = ((SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY +
//                    actorInstantiation.getIdentifier().getType().toString()));
//            known_act = act_sym.getActorDeclaration().getKnownActors();
//
//            if (act_sym.getActorDeclaration().getInitHandler() != null) {
//                init_args = act_sym.getActorDeclaration().getInitHandler().getArgs();
//                actial_init_arg_size = init_args.size();
//            }
//
//            if (known_act.size() != known_pass) {
//                int line = actorInstantiation.getLine();
//                String err = "knownactors do not match with definition";
//                errors.add(new ErrorInType(line, err));
//                return;
//            }
//            int i = 0;
//            for (Identifier knownActor : actorInstantiation.getKnownActors()) {
//                String act1 = knownActor.getType().toString();
//                String act2 = known_act.get(i).getType().toString();
////                System.out.printf(" %d: %s ---> %s\n " ,actorInstantiation.getLine(), act1,act2);
//                if (!check_first_actor_subtype_second(act1, act2)) {
//                    int line = actorInstantiation.getLine();
//                    String err = "knownactors doe not match with definition";
//                    errors.add(new ErrorInType(line, err));
//                    return;
//                }
//                i+=1;
//            }
//
//            if (actial_init_arg_size != arg_pass) {
//                int line = actorInstantiation.getLine();
//                String err = "arguments do not match with definition";
//                errors.add(new ErrorInType(line, err));
//                return;
//            }
//            i = 0;
//
//            for (Expression arg : actorInstantiation.getInitArgs()) {
//                Type arg_init = init_args.get(i).getType();
//                if (arg.getType() instanceof NoType) {
//                    continue;
//                } else {
//                    if (!(arg.getType() instanceof IntType && arg_init instanceof IntType) ||
//                            (arg.getType() instanceof StringType && arg_init instanceof StringType) ||
//                            (arg.getType() instanceof ArrayType && arg_init instanceof ArrayType) ||
//                            (arg_init instanceof BooleanType && arg_init instanceof BooleanType)) {
//                        int line = actorInstantiation.getLine();
//                        String err = "initial args does not match with definition";
//                        errors.add(new ErrorInType(line, err));
//                        return;
//                    }
//                }
//            }
//
//
//        } catch (ItemNotFoundException ignored) {
//        }


    }

    private String boolCMPOperator(String op) {
        String result = "";
        result += "if_icmp" + op + " Label" + labelGenerator + "\n";
        result += "ldc 1\n";
        result += "goto Label" + (labelGenerator + 1) + "\n";
        result += "Label" + (labelGenerator++) + ":\n";
        result += "ldc 0\n";
        result += "Label" + (labelGenerator++) + ":";
        return result;
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        if (unaryExpression == null)
            return;

        UnaryOperator operator = unaryExpression.getUnaryOperator();
        Expression operand = unaryExpression.getOperand();
        switch (operator) {
            case postinc: case preinc:
                BinaryExpression binaryExpression = new BinaryExpression(
                        operand, new IntValue(1, new IntType()), BinaryOperator.add);
                binaryExpression.setType(new IntType());
                Assign binaryAssign = new Assign(operand, binaryExpression);
                if (operator == UnaryOperator.preinc) {
                    this.visit(binaryAssign);
                    this.visitExpr(operand);
                } else {
                    this.visitExpr(operand);
                    this.visit(binaryAssign);
                }
                unaryExpression.setType(new IntType());
                break;
            case predec: case postdec:
                BinaryExpression binaryExpression1 = new BinaryExpression(operand, new IntValue(1, new IntType()), BinaryOperator.sub);
                binaryExpression1.setType(new IntType());
                Assign binaryAssign1 = new Assign(operand, binaryExpression1);
                if (operator == UnaryOperator.predec) {
                    this.visit(binaryAssign1);
                    this.visitExpr(operand);
                } else {
                    this.visitExpr(operand);
                    this.visit(binaryAssign1);
                }
                unaryExpression.setType(new IntType());
                break;
            case not:
                this.visitExpr(operand);
                String cmd = "ifne Label" + labelGenerator + "\n";
                cmd += "ldc 1\n";
                cmd += "goto Label" + (labelGenerator + 1) +"\n";
                cmd += "Label" + (labelGenerator++) + ":\n";
                cmd += "ldc 0\n";
                cmd += "Label" + (labelGenerator++) + ":";
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,cmd);
                }
                else{
                    code_generation_translator.add_command("Main",cmd);
                }
                unaryExpression.setType(new BooleanType());
                break;
            case minus:
                this.visitExpr(operand);
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,"ineg");
                }
                else{
                    code_generation_translator.add_command("Main","ineg");
                }
                unaryExpression.setType(new IntType());
                break;
        }
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        BinaryOperator operator = binaryExpression.getBinaryOperator();
        switch (operator) {
            case add:
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new IntType());
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,"iadd");
                }
                else{
                    code_generation_translator.add_command("Main","iadd");
                }
                break;
            case sub:
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new IntType());
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,"isub");
                }
                else{
                    code_generation_translator.add_command("Main","isub");
                }
                break;
            case mult:
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new IntType());
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,"imul");
                }
                else{
                    code_generation_translator.add_command("Main","imul");
                }
                break;
            case div:
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new IntType());
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,"idiv");
                }
                else{
                    code_generation_translator.add_command("Main","idiv");
                }
                break;
            case mod:
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new IntType());
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,"irem");
                }
                else{
                    code_generation_translator.add_command("Main","irem");
                }
                break;
            case eq:
                Expression left = binaryExpression.getLeft();
                Expression right = binaryExpression.getRight();
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new BooleanType());

                if (left.getType() instanceof  IntType || left.getType() instanceof BooleanType){
                    if(!in_main){
                        code_generation_translator.add_command(curr_actor_name,boolCMPOperator("ne"));
                    }
                    else{
                        code_generation_translator.add_command("Main",boolCMPOperator("ne"));
                    }
                }
                else{
                    String message = "invokestatic java/util/Objects/equals(Ljava/lang/Object;Ljava/lang/Object;)Z";
                    if(!in_main){
                        code_generation_translator.add_command(curr_actor_name,message);
                    }
                    else{
                        code_generation_translator.add_command("Main",message);
                    }
                }
                break;
            case neq:
                Expression left1 = binaryExpression.getLeft();
                Expression right1 = binaryExpression.getRight();
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new BooleanType());
                if (left1.getType() instanceof  IntType || left1.getType() instanceof BooleanType){
                    if(!in_main){
                        code_generation_translator.add_command(curr_actor_name,boolCMPOperator("eq"));
                    }
                    else{
                        code_generation_translator.add_command("Main",boolCMPOperator("eq"));
                    }
                }
                else{
                    String message = "invokestatic java/util/Objects/equals(Ljava/lang/Object;Ljava/lang/Object;)Z";
                    if(!in_main){
                        code_generation_translator.add_command(curr_actor_name,message);
                    }
                    else{
                        code_generation_translator.add_command("Main",message);
                    }
                }
                break;
            case gt:
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new BooleanType());
                if(!in_main) {
                    code_generation_translator.add_command(curr_actor_name, boolCMPOperator("le"));
                }
                else{
                    code_generation_translator.add_command("Main", boolCMPOperator("le"));
                }
                break;
            case lt:
                this.visitExpr(binaryExpression.getLeft());
                this.visitExpr(binaryExpression.getRight());
                binaryExpression.setType(new BooleanType());
                if(!in_main) {
                    code_generation_translator.add_command(curr_actor_name, boolCMPOperator("ge"));
                }
                else{
                    code_generation_translator.add_command("Main", boolCMPOperator("ge"));
                }
                break;
            case or:
                String cmd;
                this.visitExpr(binaryExpression.getRight());
                cmd = "ifne Label" + labelGenerator+"\n";
                this.visitExpr(binaryExpression.getLeft());
                cmd += "ifeq Label" + (labelGenerator + 1) +"\n";
                cmd += "Label" + labelGenerator + ":" +"\n";
                cmd += "ldc 1\n";
                cmd += "goto Label" + (labelGenerator + 2) + "\n";
                cmd += "Label" + (labelGenerator + 1) + ":\n";
                cmd += "ldc 0\n";
                cmd += "Label" + (labelGenerator + 2) + ":";
                labelGenerator += 3;
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,cmd);
                }
                else{
                    code_generation_translator.add_command("Main",cmd);
                }
                binaryExpression.setType(new BooleanType());
                break;
            case and:
                String cmdd;
                this.visitExpr(binaryExpression.getRight());
                cmdd = "ifeq Label" + labelGenerator+"\n";
                this.visitExpr(binaryExpression.getLeft());
                cmdd += "ifeq Label" + (labelGenerator) +"\n";
                cmdd += "ldc 1\n";
                cmdd += "goto Label" + (labelGenerator + 1) + "\n";
                cmdd += "Label" + (labelGenerator++) + ":\n";
                cmdd += "ldc 0\n";
                cmdd += "Label" + (labelGenerator++) + ":";
                if(!in_main){
                    code_generation_translator.add_command(curr_actor_name,cmdd);
                }
                else{
                    code_generation_translator.add_command("Main",cmdd);
                }
                binaryExpression.setType(new BooleanType());
                break;
            case assign:
                Assign binaryAssign = new Assign(binaryExpression.getLeft(), binaryExpression.getRight());
                this.visit(binaryAssign);
                this.visitExpr(binaryExpression.getLeft());
                break;
        }
    }


    @Override
    public void visit(Identifier identifier) {
        if (identifier == null)
            return;
        curr_id = identifier;
        if (!msg_handler_id && !msg_handler_id_call && !act_var_id) {
            if (this.in_main) {
                Type the_type = null;
                try {
                    the_type = this.type_getter_from_main(identifier);

                    try {
                        Identifier temp_id = new Identifier(the_type.toString());
                        Type temp_type = this.type_getter_from_actor_items(temp_id);

                    } catch (ItemNotFoundException e1) {
                        int line = identifier.getLine();
                        String err = "actor " + the_type.toString() + " is not declared";
                        the_type = new NoType();

                        the_type = new NoType();
                    }
                } catch (ItemNotFoundException e) {
                    int line = identifier.getLine();
                    String err = "variable " + identifier.getName() + " is not declared";
                    the_type = new NoType();

                }
                identifier.setType(the_type);

            } else if (this.in_actor) {
                Type the_type = null;
                if (this.in_msg_handler || in_initial) {
                    try {
                        the_type = this.type_getter_from_whole_actor(identifier);
                    } catch (ItemNotFoundException e1) {
                        int line = identifier.getLine();
                        String err = "variable " + identifier.getName() + " is not declared";

                        the_type = new NoType();
                    }
                } else {
                    try {
                        the_type = this.type_getter_from_act(identifier);
                    } catch (ItemNotFoundException e2) {
                        int line = identifier.getLine();
                        String err = new String("varibale " + identifier.getName() + " is not declared");

                        the_type = new NoType();
                    }
                }
                identifier.setType(the_type);

            } else {
                Type the_type = null;
                try {
                    the_type = this.type_getter_from_actor_items(identifier);
                } catch (ItemNotFoundException e3) {
                    int line = identifier.getLine();
                    String err = "actor " + identifier.getName() + " is not declared";

                    the_type = new NoType();
                }
                identifier.setType(the_type);
            }
        }



        if (!in_var_dec && !is_left_val && !in_arr_call && !msg_handler_id &&
                !msg_handler_id_call && !act_var_id && !actor_id && !actor_var_access) {
            if ((in_msg_handler || in_initial) && !in_handler_call) {
                make_load_identifier_code(curr_actor_name, identifier);
            }
        }

    }

    public void make_load_identifier_code(String curr_actor_name, Identifier identifier) {
        int index = -1;
        if (in_initial) {
            if (map_var_in_initial.containsKey(identifier.getName())) {
                index = map_var_in_initial.get(identifier.getName());
            }
        }
        if (in_msg_handler) {
            if (map_var_in_handler.containsKey(identifier.getName())) {
                index = map_var_in_handler.get(identifier.getName());
            }
        }
        if (index != -1) {
            if (identifier.getType() instanceof IntType || identifier.getType() instanceof BooleanType) {
                if (index <= 3) {
                    code_generation_translator.add_command(curr_actor_name, "iload_" + Integer.toString(index));
                } else {
                    code_generation_translator.add_command(curr_actor_name, "iload " + Integer.toString(index));
                }
            } else {
                if (index <= 3) {
                    code_generation_translator.add_command(curr_actor_name, "aload_" + Integer.toString(index));
                } else {
                    code_generation_translator.add_command(curr_actor_name, "aload " + Integer.toString(index));
                }
            }
        }

        // not in msg handler so in field;  we should get filed
        else {
            for(int i=0;i<curr_act_dec.getActorVars().size();i++){
                if(curr_act_dec.getActorVars().get(i).getIdentifier().getName().equals(identifier.getName())){
                    code_generation_translator.add_command(curr_actor_name, "aload_0\n" +
                            "getfield " + curr_actor_name + "/" + identifier.getName() + " " +
                            code_generation_translator.get_type_code_generation_equivalent(
                                    curr_act_dec.getActorVars().get(i).getIdentifier().getType()));
                    break;
                }
            }
        }
    }


    @Override
    public void visit(Self self) {
    }

    @Override
    public void visit(Sender sender) {
    }

    @Override
    public void visit(BooleanValue value) {
        value.setType(new BooleanType());
        if(!in_main) {
            code_generation_translator.add_command_for_boolean_value(curr_actor_name, value);
        }
        else {
            code_generation_translator.add_command_for_boolean_value("Main", value);
        }
    }

    @Override
    public void visit(IntValue value) {
        value.setType(new IntType());
        if(!in_main) {
            code_generation_translator.add_command_for_int_value(curr_actor_name, value);
        }
        else {
            code_generation_translator.add_command_for_int_value("Main", value);
        }
    }

    @Override
    public void visit(StringValue value) {
        value.setType(new StringType());
        if(!in_main) {
            code_generation_translator.add_command_for_string_value(curr_actor_name, value);
        }
        else {
            code_generation_translator.add_command("Main", "ldc " + value.getConstant());
        }
    }


    @Override
    public void visit(Block block) {
        if (block == null)
            return;
        for (Statement statement : block.getStatements())
            visitStatement(statement);
    }

    @Override
    public void visit(Conditional conditional) {
        int localLabel = labelGenerator;
        labelGenerator += 2;
        String cmd;
        this.visitExpr(conditional.getExpression());
        cmd = "ifeq Label" + localLabel;
        code_generation_translator.add_command(curr_actor_name,cmd);
        if (conditional.getThenBody() != null)
            this.visitStatement(conditional.getThenBody());
        cmd = "goto Label" + (localLabel + 1) +"\n";
        cmd += "Label" + localLabel + ":";
        code_generation_translator.add_command(curr_actor_name,cmd);
        if (conditional.getElseBody() != null)
            this.visitStatement(conditional.getElseBody());
        cmd = "Label" + (localLabel + 1) + ":";
        code_generation_translator.add_command(curr_actor_name,cmd);
    }


    @Override
    public void visit(For loop) {
        if (loop.getInitialize() != null)
            this.visit(loop.getInitialize());
        firstLoopLabel = labelGenerator;
        exitLoopLabel = labelGenerator + 1;
        labelGenerator += 2;
        code_generation_translator.add_command(curr_actor_name,"Label" + firstLoopLabel + ":");
        if (loop.getCondition() != null)
            this.visitExpr(loop.getCondition());
        code_generation_translator.add_command(curr_actor_name,"ifeq Label" + exitLoopLabel);
        if (loop.getBody() != null)
            this.visitStatement(loop.getBody());
        if (loop.getUpdate() != null)
            this.visit(loop.getUpdate());
        code_generation_translator.add_command(curr_actor_name,"goto Label" + firstLoopLabel);
        code_generation_translator.add_command(curr_actor_name,"Label" + exitLoopLabel +":");
    }

    @Override
    public void visit(Break breakLoop) {
        code_generation_translator.add_command(curr_actor_name,"goto Label" + exitLoopLabel);
    }

    @Override
    public void visit(Continue continueLoop) {
        code_generation_translator.add_command(curr_actor_name,"goto Label" + firstLoopLabel);
    }


    @Override
    public void visit(Print print) {
        if (print == null)
            return;
        this.code_generation_translator.get_static_print(curr_actor_name);
        visitExpr(print.getArg());
        if (print.getArg() instanceof  ArrayCall) {
            this.code_generation_translator.print_array_value(curr_actor_name);
        }
        else if (print.getArg().getType().toString().equals("string")) {
            this.code_generation_translator.print_string_value(curr_actor_name);
        }
        else if (print.getArg().getType().toString().equals("int")) {
            this.code_generation_translator.print_int_value(curr_actor_name);
        }

        else if (print.getArg().getType().toString().equals("boolean")) {
            this.code_generation_translator.print_boolean_value(curr_actor_name);
        }

    }


    @Override
    public void visit(Assign assign) {
//        if(assign.getlValue() instanceof ArrayCall){
//            System.out.println("in line " + assign.getLine() + "  left value is arraycall");
//        }
//        else if(assign.getlValue() instanceof Identifier){
//            System.out.println("in line " + assign.getLine() + "  left value is identifier");
//        }
//        else if(assign.getlValue() instanceof ActorVarAccess){
//            System.out.println("in line " + assign.getLine() + "  left value is actorvaraccess");
//        }
        in_assign = true;
        if (assign.getlValue() instanceof ArrayCall) {
            is_left_val = true;
            visitExpr(assign.getlValue());
            is_left_val = false;
            visitExpr(assign.getrValue());
            code_generation_translator.add_command(curr_actor_name, "iastore");
        } else if (assign.getlValue() instanceof Identifier) {
            this.is_left_val = true;
            visitExpr(assign.getlValue());
            this.is_left_val = false;
            int index = -1;
            if (in_initial) {
                if (map_var_in_initial.containsKey(curr_id.getName())) {
                    index = map_var_in_initial.get(curr_id.getName());
                }
            } else if (in_msg_handler) {
                if (map_var_in_handler.containsKey(curr_id.getName())) {
                    index = map_var_in_handler.get(curr_id.getName());
                }
            }
            if (index == -1) {
                code_generation_translator.add_command(curr_actor_name, "aload_0");
            }
            Identifier left = curr_id;
            visitExpr(assign.getrValue());
            if (index != -1) {
                if (left.getType() instanceof IntType ||
                        curr_id.getType() instanceof BooleanType) {
                    if (index <= 3) {
                        code_generation_translator.add_command(curr_actor_name, "istore_" + Integer.toString(index));
                    } else {
                        code_generation_translator.add_command(curr_actor_name, "istore " + Integer.toString(index));
                    }
                } else {
                    if (index <= 3) {
                        code_generation_translator.add_command(curr_actor_name, "astore_" + Integer.toString(index));
                    } else {
                        code_generation_translator.add_command(curr_actor_name, "astore " + Integer.toString(index));
                    }
                }
            } else {
                code_generation_translator.add_command(curr_actor_name, "putfield " + curr_actor_name + "/" +
                        left.getName() + " " +
                        code_generation_translator.get_type_code_generation_equivalent(left.getType()));
            }
        } else if (assign.getlValue() instanceof ActorVarAccess) {
            this.is_left_val = true;
            visitExpr(assign.getlValue());
            this.is_left_val = false;
            code_generation_translator.add_command(curr_actor_name, "aload_0");
            visitExpr(assign.getrValue());
            curr_id = ((ActorVarAccess) (assign.getlValue())).getVariable();
            code_generation_translator.add_command(curr_actor_name, "putfield " + curr_actor_name + "/" +
                    curr_id.getName() + " " +
                    code_generation_translator.get_type_code_generation_equivalent(curr_id.getType()));
        }
        in_assign = false;
    }



    @Override
    public void visit(ArrayCall arrayCall) {
        in_arr_call = true;
        visitExpr(arrayCall.getArrayInstance());
        in_arr_call = false;
        make_load_identifier_code(curr_actor_name,curr_id);
        visitExpr(arrayCall.getIndex());
        if (!this.is_left_val) {
            code_generation_translator.add_command(curr_actor_name,"iaload");
        }
    }


    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        if (actorVarAccess == null)
            return;
        if (in_main) {
            act_var_id = true;
        }
        actor_var_access = true;
        visitExpr(actorVarAccess.getSelf());
        visitExpr(actorVarAccess.getVariable());
        actor_var_access = false;
        act_var_id = false;
        actorVarAccess.setType(actorVarAccess.getVariable().getType());
        if(!is_left_val && !(actorVarAccess.getVariable().getType() instanceof ArrayType)){
            code_generation_translator.add_command(curr_actor_name,"aload_0");
            String cmd = "getfield " + curr_actor_name +"/" + actorVarAccess.getVariable().getName() +" " +
                    code_generation_translator.get_type_code_generation_equivalent(actorVarAccess.getVariable().getType());
            code_generation_translator.add_command(curr_actor_name,cmd);
        }
    }


    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        if (msgHandlerCall == null) {
            return;
        }
        try {
            msg_handler_id_call = true;
            in_handler_call = true;
            visitExpr(msgHandlerCall.getInstance());
            if(msgHandlerCall.getInstance() instanceof Self){
                code_generation_translator.add_command(curr_actor_name,"aload_0");
            }
            else if(msgHandlerCall.getInstance() instanceof Sender){
                code_generation_translator.add_command(curr_actor_name , "aload_1");
            }
            else if(msgHandlerCall.getInstance() instanceof Identifier){
                code_generation_translator.add_command(curr_actor_name,"aload_0");
                code_generation_translator.add_command(curr_actor_name,"getfield " + curr_actor_name + "/" +
                        ((Identifier) msgHandlerCall.getInstance()).getName() + " L" +
                        known_actors.get(((Identifier) msgHandlerCall.getInstance()).getName()) +";");
            }
            visitExpr(msgHandlerCall.getMsgHandlerName());
            msg_handler_id_call = false;
            in_handler_call = false;
            code_generation_translator.add_command(curr_actor_name,"aload_0");
            for (Expression argument : msgHandlerCall.getArgs())
                visitExpr(argument);
            if(msgHandlerCall.getInstance() instanceof Self){
                String cmd = "invokevirtual " + curr_actor_name +
                        "/send_" + msgHandlerCall.getMsgHandlerName().getName() + "(LActor;";
                for(int i=0;i<msgHandlerCall.getArgs().size();i++){
                    cmd +=code_generation_translator.get_type_code_generation_equivalent(
                            msgHandlerCall.getArgs().get(i).getType());
                }
                cmd+=")V";
                code_generation_translator.add_command(curr_actor_name,cmd);
            }

            else if(msgHandlerCall.getInstance() instanceof Sender){
                String cmd = "invokevirtual Actor" +
                        "/send_" + msgHandlerCall.getMsgHandlerName().getName() + "(LActor;";
                for(int i=0;i<msgHandlerCall.getArgs().size();i++){
                    cmd +=code_generation_translator.get_type_code_generation_equivalent(
                            msgHandlerCall.getArgs().get(i).getType());
                }
                cmd+=")V";
                code_generation_translator.add_command(curr_actor_name,cmd);
            }
            else if(msgHandlerCall.getInstance() instanceof Identifier){
                String cmd = "invokevirtual " + known_actors.get(((Identifier) msgHandlerCall.getInstance()).getName()) +
                        "/send_" + msgHandlerCall.getMsgHandlerName().getName() + "(LActor;";
                for(int i=0;i<msgHandlerCall.getArgs().size();i++){
                    cmd +=code_generation_translator.get_type_code_generation_equivalent(
                            msgHandlerCall.getArgs().get(i).getType());
                }
                cmd+=")V";
                code_generation_translator.add_command(curr_actor_name,cmd);
            }

        }
        catch(NullPointerException npe) {
            System.out.println("null pointer exception occurred");
        }
    }


}



