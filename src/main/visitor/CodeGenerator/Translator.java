package main.visitor;

import java.util.ArrayList;
import java.util.*;
import java.io.*;

import main.ast.node.declaration.handler.InitHandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.expression.*;
import main.ast.node.declaration.*;
import main.ast.node.expression.operators.*;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.type.Type;
import main.ast.type.actorType.ActorType;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.symbolTable.*;
import main.symbolTable.itemException.*;
import main.symbolTable.symbolTableVariableItem.SymbolTableActorVariableItem;
import main.symbolTable.symbolTableVariableItem.SymbolTableVariableItem;

import javax.print.DocFlavor;
import javax.swing.*;
import javax.swing.text.StyledEditorKit;


public class Translator {
    private static final String FOLDER = "./jasmin/";
    private HashMap<String, ArrayList<String>> commands;

    public Translator() {
        this.commands = new HashMap<String, ArrayList<String>>();
    }

    public void jumpToLabel(String actor_name, String label_str, int lable_number){
        commands.get(actor_name).add("goto "+label_str+"_"+Integer.toString(lable_number));
    }


    public void add_loading_of_this(String actor_name) {
        commands.get(actor_name).add("aload 0 ;this");
    }

    public void get_actor_filed(String actor_name, String actor_name_holding_var, String var_name, Type type) {
        commands.get(actor_name).add("aload 0 ;this");
        commands.get(actor_name).add("getfield " + actor_name_holding_var + "/" + var_name + " " +
                this.get_type_code_generation_equivalent(type));
    }

    public void put_actor_field(String actor_name, String actor_name_holding_var, String var_name, Type type) {
        commands.get(actor_name).add("putfield " + actor_name_holding_var + "/" + var_name + " " +
                this.get_type_code_generation_equivalent(type));
    }

    public void get_static_print(String actor_name) {
        commands.get(actor_name).add("getstatic java/lang/System/out Ljava/io/PrintStream;");
    }

    public void print_string_value(String actor_name) {
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
    }

    public void print_int_value(String actor_name) {
        ArrayList<String> c = this.commands.get(actor_name);

        c.add("invokevirtual java/io/PrintStream/println(I)V");
    }

    public void print_array_value(String actor_name) {
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("invokestatic java/util/Arrays.toString([I)Ljava/lang/String;");
        c.add("invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V");
    }

    public void print_boolean_value(String actor_name) {
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("invokevirtual java/io/PrintStream/println(Z)V");
    }

    public void create_new_array(String actor_name, int length) {
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("ldc " + Integer.toString(length));
        c.add("newarray   int");
    }


    public void put_constant_int_on_top_of_stack(String actor_name, int value) {
        commands.get(actor_name).add("ldc " + Integer.toString(value));
    }

    public void put_constant_string_on_top_of_stack(String actor_name, String value) {
        commands.get(actor_name).add("ldc " + value);
    }

    public void put_constant_bool_on_top_of_stack(String actor_name, Boolean value) {
        if (value) {
            commands.get(actor_name).add("iconst_1");
        } else {
            commands.get(actor_name).add("iconst_0");
        }
    }

    public void create_set_known_actor_method_in_actor_file(String actor_name, ArrayList<VarDeclaration> known_actors) {
        ArrayList<String> c = this.commands.get(actor_name);
        for (int i = 0; i < known_actors.size(); i++) {
            String known_actor_name = known_actors.get(i).getIdentifier().getName();
            Type known_actor_type = known_actors.get(i).getType();
        }
    }

    public String convert_args_to_string(ArrayList<VarDeclaration> args) {
        ArrayList<String> args_string = new ArrayList<String>();
        for (int i = 0; i < args.size(); i++) {
            Type type = args.get(i).getType();
            String this_type = get_type_code_generation_equivalent(type);
            args_string.add(this_type);
        }
        return String.join("", args_string);
    }

    public String get_type_code_generation_equivalent(Type type) {
        if (type instanceof IntType) {
            return "I";
        } else if (type instanceof BooleanType) {
            return "Z";
        } else if (type instanceof StringType) {
            return "Ljava/lang/String;";
        } else if (type instanceof ArrayType) {
            return "[I";
        } else if (type instanceof ActorType) {
            return "L" + type.toString() + ";";
        }
        return "";
    }

    public void perform_var_declaration(String actor_name, String var_name, Type type) {
        String type_of_this;
        type_of_this = get_type_code_generation_equivalent(type);
        commands.get(actor_name).add(".field " + var_name + " " + type_of_this);
    }


    public void create_file_for_actor(String actor_name, ActorDeclaration actorDeclaration) {
        this.commands.put(actor_name, new ArrayList<String>());
        ArrayList<String> c = this.commands.get(actor_name);
        c.add(".class public " + actor_name);
        c.add(".super Actor");
        c.add("\n");
    }

    public void add_known_actor_to_field_of_actor_in_file(String actor_name, VarDeclaration known_actor) {
        ArrayList<String> c = this.commands.get(actor_name);
        this.perform_var_declaration(actor_name, known_actor.getIdentifier().getName(), known_actor.getType());
    }

    public void add_actor_var_to_filed_of_actor_in_file(String actor_name, VarDeclaration actor_var) {
        ArrayList<String> c = this.commands.get(actor_name);
        this.perform_var_declaration(actor_name, actor_var.getIdentifier().getName(), actor_var.getType());
    }

    public void create_constructor_in_actor_file(String actor_name , ArrayList<VarDeclaration> act_vars) {
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("\n");
        c.add(".method public <init>(I)V");
        c.add(".limit stack 2");
        c.add(".limit locals 2");
        c.add("aload_0");
        c.add("iload_1");
        c.add("invokespecial Actor/<init>(I)V");
        for(int i=0;i<act_vars.size();i++){
            String var_name = act_vars.get(i).getIdentifier().getName();
            Type var_type = act_vars.get(i).getIdentifier().getType();
            c.add("aload_0");
            if(var_type instanceof BooleanType || var_type instanceof  IntType){
                c.add("iconst_0");
                c.add("putfield " + actor_name +"/" + var_name +" " + get_type_code_generation_equivalent(var_type));

            }

            else if(var_type instanceof StringType){
                c.add("ldc \"\"");
                c.add("putfield " + actor_name +"/" + var_name +" " + get_type_code_generation_equivalent(var_type));
            }
            else if(var_type instanceof ArrayType){
                int size = ((ArrayType)act_vars.get(i).getType()).getSize();
                c.add("ldc " + Integer.toString(size));
                c.add("new array int");
                c.add("putfield " + actor_name +"/" + var_name +" " + get_type_code_generation_equivalent(var_type));
            }
        }

        c.add("return");
        c.add(".end method");
    }

    void create_a_folder_for_output_files() {
        File newFolder = new File(FOLDER);
        boolean created = newFolder.mkdir();
    }

    void create_and_write_to_file(String actor_name) {
        try {
            File file = new File(FOLDER + actor_name + ".j");
            PrintWriter writer = new PrintWriter(FOLDER + actor_name + ".j");
            for (int i = 0; i < this.commands.get(actor_name).size(); i++) {
                writer.println(this.commands.get(actor_name).get(i));
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Unable to create or write to file");
        }

    }

    public void printTheCommands() {
        create_a_folder_for_output_files();
        for (String name : this.commands.keySet()) {
            create_and_write_to_file(name);
       }
       add_Actor_file_in_jasmin_folder();
        add_default_actor_in_jasmin_folder();
        add_message_in_jasmin_folder();
    }


    public void create_actor_msg_handler_seperate_file(String actor_name, String msg_handler_name,
                                                       ArrayList<VarDeclaration> args) {

        this.commands.put(actor_name + "_" + msg_handler_name, new ArrayList<String>());
        ArrayList<String> c = this.commands.get(actor_name + "_" + msg_handler_name);
        c.add(".class public " + actor_name + "_" + msg_handler_name);
        c.add(".super Message");

        c.add("\n");

        for (int i = 0; i < args.size(); i++) {
            c.add(".field private " + args.get(i).getIdentifier().getName()+" " +
                    get_type_code_generation_equivalent(args.get(i).getType()));
        }

        c.add(".field private receiver " + "L" + actor_name + ";");
        c.add(".field private sender LActor;");

        c.add("\n");

        c.add(".method public <init>(L" + actor_name + ";LActor;" + convert_args_to_string(args) + ")V");
        c.add(".limit stack 2");
        c.add(".limit locals " + Integer.toString(3 + args.size()));
        c.add("aload_0");
        c.add("invokespecial Message/<init>()V ");
        c.add("aload_0");
        c.add("aload_1");
        c.add("putfield " + actor_name + "_" + msg_handler_name + "/receiver L" + actor_name + ";");
        c.add("aload_0");
        c.add("aload_2");
        c.add("putfield " + actor_name + "_" + msg_handler_name + "/sender LActor;");
        for (int i = 0; i < args.size(); i++) {
            c.add("aload_0");
            if (i + 3 == 3) {
                if (args.get(i).getType() instanceof IntType || args.get(i).getType() instanceof BooleanType) {
                    c.add("iload_3");
                }
                else if(args.get(i).getType() instanceof  StringType || args.get(i).getType() instanceof ArrayType){
                    c.add("aload_3");
                }
            } else {
                if (args.get(i).getType() instanceof IntType || args.get(i).getType() instanceof BooleanType) {
                    c.add("iload " + Integer.toString(i+3));
                }
                else if(args.get(i).getType() instanceof  StringType || args.get(i).getType() instanceof ArrayType){
                    c.add("aload " + Integer.toString(i+3));
                }
            }
            c.add("putfield " + actor_name + "_" + msg_handler_name + "/" + args.get(i).getIdentifier().getName() + " " +
                    get_type_code_generation_equivalent(args.get(i).getType()));
        }
        c.add("return");
        c.add(".end method");

        c.add("\n");
        c.add(".method public execute()V\n" +
                ".limit stack " + Integer.toString(2 + args.size()) + "\n" +
                ".limit locals 1");
        c.add("aload_0\n" +
                "getfield " + actor_name + "_" + msg_handler_name + "/receiver L" + actor_name + ";");
        c.add("aload_0\n" +
                "getfield " + actor_name + "_" + msg_handler_name + "/sender LActor;");
        for (int i = 0; i < args.size(); i++) {
            c.add("aload_0");
            c.add("getfield " + actor_name + "_" + msg_handler_name + "/" + args.get(i).getIdentifier().getName() + " " +
                    get_type_code_generation_equivalent(args.get(i).getType()));
        }
        c.add("invokevirtual " + actor_name + "/" + msg_handler_name + "(LActor;" + convert_args_to_string(args) + ")V");
        c.add("return");
        c.add(".end method");


        try {
            File file = new File(FOLDER + actor_name + "_" + msg_handler_name + ".j");
            PrintWriter writer = new PrintWriter(FOLDER + actor_name + "_" + msg_handler_name + ".j");
            for (int i = 0; i < this.commands.get(actor_name + "_" + msg_handler_name).size(); i++) {
                writer.println(this.commands.get(actor_name + "_" + msg_handler_name).get(i));
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Unable to create or write to file");
        }
    }



    public void add_send_type_of_msg_handler_in_default_actor(String actor_name, String msg_handler_name,
                                                              ArrayList<VarDeclaration> args){
        ArrayList<String> c = this.commands.get("DefaultActor");
        c.add("\n.method public send_" + msg_handler_name +"(LActor;" + convert_args_to_string(args) +")V");
        c.add(".limit stack 2\n" +
                ".limit locals " + Integer.toString(2 + args.size()));
        c.add("getstatic java/lang/System/out Ljava/io/PrintStream;");
        c.add("ldc \"there is no msghandler named " + msg_handler_name+" in sender\"");
        c.add("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n" +
                "return\n" +
                ".end method");
    }

    public void add_set_known_actors_method_in_actor_file(String actor_name, ArrayList<VarDeclaration> known_actors) {
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("\n");
        c.add(".method public setKnownActors(" + convert_args_to_string(known_actors) + ")V");
        c.add(".limit stack 2\n" +
                ".limit locals " + Integer.toString(1 + known_actors.size()));
        for (int i = 0; i < known_actors.size(); i++) {
            c.add("aload_0");
            if (i+1 <= 3) {
                c.add("aload_" + Integer.toString(i+1));
            } else {
                c.add("aload " + Integer.toString(i+1));
            }
            c.add("putfield " + actor_name + "/" + known_actors.get(i).getIdentifier().getName() + " L" +
                    known_actors.get(i).getIdentifier().getType() + ";");
        }
        c.add("return\n" +
                ".end method");
    }


    public void add_Actor_file_in_jasmin_folder(){
        this.commands.put("Actor", new ArrayList<String>());
        ArrayList<String> c = this.commands.get("Actor");
        c.add(".class public Actor\n" +
                ".super DefaultActor\n" +
                "\n" +
                ".field private queue Ljava/util/ArrayList;\n" +
                ".signature \"Ljava/util/ArrayList<LMessage;>;\"\n" +
                ".end field\n" +
                ".field private lock Ljava/util/concurrent/locks/ReentrantLock;\n" +
                ".end field\n" +
                ".field queueSize I\n" +
                ".end field\n" +
                "\n" +
                ".method public <init>(I)V\n" +
                ".limit stack 3\n" +
                ".limit locals 2\n" +
                "aload_0\n" +
                "invokespecial DefaultActor/<init>()V\n" +
                "aload_0\n" +
                "new java/util/ArrayList\n" +
                "dup\n" +
                "invokespecial java/util/ArrayList/<init>()V\n" +
                "putfield Actor/queue Ljava/util/ArrayList;\n" +
                "aload_0\n" +
                "new java/util/concurrent/locks/ReentrantLock\n" +
                "dup\n" +
                "invokespecial java/util/concurrent/locks/ReentrantLock/<init>()V\n" +
                "putfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;\n" +
                "aload_0\n" +
                "iload_1\n" +
                "putfield Actor/queueSize I\n" +
                "return\n" +
                ".end method\n" +
                "\n" +
                ".method public run()V\n" +
                ".limit stack 2\n" +
                ".limit locals 1\n" +
                "Label0:\n" +
                "aload_0\n" +
                "getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;\n" +
                "invokevirtual java/util/concurrent/locks/ReentrantLock/lock()V\n" +
                "aload_0\n" +
                "getfield Actor/queue Ljava/util/ArrayList;\n" +
                "invokevirtual java/util/ArrayList/isEmpty()Z\n" +
                "ifne Label31\n" +
                "aload_0\n" +
                "getfield Actor/queue Ljava/util/ArrayList;\n" +
                "iconst_0\n" +
                "invokevirtual java/util/ArrayList/remove(I)Ljava/lang/Object;\n" +
                "checkcast Message\n" +
                "invokevirtual Message/execute()V\n" +
                "Label31:\n" +
                "aload_0\n" +
                "getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;\n" +
                "invokevirtual java/util/concurrent/locks/ReentrantLock/unlock()V\n" +
                "goto Label0\n" +
                ".end method\n" +
                "\n" +
                ".method public send(LMessage;)V\n" +
                ".limit stack 2\n" +
                ".limit locals 2\n" +
                "aload_0\n" +
                "getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;\n" +
                "invokevirtual java/util/concurrent/locks/ReentrantLock/lock()V\n" +
                "aload_0\n" +
                "getfield Actor/queue Ljava/util/ArrayList;\n" +
                "invokevirtual java/util/ArrayList/size()I\n" +
                "aload_0\n" +
                "getfield Actor/queueSize I\n" +
                "if_icmpge Label30\n" +
                "aload_0\n" +
                "getfield Actor/queue Ljava/util/ArrayList;\n" +
                "aload_1\n" +
                "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n" +
                "pop\n" +
                "Label30:\n" +
                "aload_0\n" +
                "getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;\n" +
                "invokevirtual java/util/concurrent/locks/ReentrantLock/unlock()V\n" +
                "return\n" +
                ".end method");
        try {
            File file = new File(FOLDER + "Actor.j");
            PrintWriter writer = new PrintWriter(FOLDER + "Actor.j");
            for (int i = 0; i < this.commands.get("Actor").size(); i++) {
                writer.println(this.commands.get("Actor").get(i));
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Unable to create or write to file");
        }


    }

    public void add_default_actor_in_jasmin_folder() {
        this.commands.put("DefaultActor", new ArrayList<String>());
        ArrayList<String> c = this.commands.get("DefaultActor");
        c.add(".class public DefaultActor\n" +
                ".super java/lang/Thread\n" +
                "\n" +
                ".method public <init>()V\n" +
                ".limit stack 1\n" +
                ".limit locals 1\n" +
                "aload_0\n" +
                "invokespecial java/lang/Thread/<init>()V\n" +
                "return\n" +
                ".end method\n" +
                "\n");
    }

    public void make_default_actor_file(){
        try {
            File file = new File(FOLDER + "DefaultActor.j");
            PrintWriter writer = new PrintWriter(FOLDER + "DefaultActor.j");
            for (int i = 0; i < this.commands.get("DefaultActor").size(); i++) {
                writer.println(this.commands.get("DefaultActor").get(i));
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Unable to create or write to file");
        }
    }


    public void add_message_in_jasmin_folder(){
        this.commands.put("Message", new ArrayList<String>());
        ArrayList<String> c = this.commands.get("Message");
        c.add(".class public abstract Message\n" +
                ".super java/lang/Object\n" +
                "\n" +
                ".method public <init>()V\n" +
                ".limit stack 1\n" +
                ".limit locals 1\n" +
                "0: aload_0\n" +
                "1: invokespecial java/lang/Object/<init>()V\n" +
                "4: return\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract execute()V\n" +
                ".end method");
        try {
            File file = new File(FOLDER + "Message.j");
            PrintWriter writer = new PrintWriter(FOLDER + "Message.j");
            for (int i = 0; i < this.commands.get("Message").size(); i++) {
                writer.println(this.commands.get("Message").get(i));
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Unable to create or write to file");
        }
    }

    public void make_send_of_msg_handler_in_actor_file(String actor_name , String msg_handler_name ,
                                                       ArrayList<VarDeclaration> args){
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("\n");
        c.add(".method public send_"+msg_handler_name + "(LActor;" + convert_args_to_string(args) +")V");
        c.add(".limit stack 16");
        c.add(".limit locals " + Integer.toString(2 + args.size()));
        c.add("aload_0");
        c.add("new " + actor_name +"_" + msg_handler_name);
        c.add("dup");
        c.add("aload_0");
        c.add("aload_1");
        for(int i=0;i<args.size();i++){
            if (args.get(i).getType() instanceof IntType  || args.get(i).getType() instanceof BooleanType){
                if (i+2<=3){
                    c.add("iload_" + Integer.toString(i+2));
                }
                else{
                    c.add("iload" + Integer.toString(i));
                }
            }
            else if(args.get(i).getType() instanceof StringType || args.get(i).getType() instanceof ArrayType){
                if (i+2<=3){
                    c.add("aload_" + Integer.toString(i+2));
                }
                else{
                    c.add("aload" + Integer.toString(i));
                }
            }
        }
        c.add("invokespecial " + actor_name +"_" + msg_handler_name +"/<init>(L" + actor_name +";LActor;" +
                convert_args_to_string(args) + ")V");
        c.add("invokevirtual " + actor_name + "/send(LMessage;)V");
        c.add("return");
        c.add(".end method");
    }


    public void  create_main_file() {
        this.commands.put("Main", new ArrayList<String>());
        ArrayList<String> c = this.commands.get("Main");
        c.add(".class public Main\n" +
                ".super java/lang/Object");
        c.add("\n.method public <init>()V\n" +
                ".limit stack 1\n" +
                ".limit locals 1\n" +
                "aload_0\n" +
                "invokespecial java/lang/Object/<init>()V\n" +
                "return\n" +
                ".end method");
        c.add("\n.method public static main([Ljava/lang/String;)V\n" +
                ".limit stack 16\n" +
                ".limit locals 16");
    }

    public void make_actor_instantiation_in_main(String type_actor , int index_actor_in_main){
        ArrayList<String> c = this.commands.get("Main");
        c.add("new " + type_actor);
        c.add("dup");
        int queue_size = 0;
        try {
            SymbolTableActorItem act_sym = (SymbolTableActorItem)SymbolTable.root.get(SymbolTableActorItem.STARTKEY +
                    type_actor);
            queue_size = act_sym.getActorDeclaration().getQueueSize();
        } catch (ItemNotFoundException ignored) {
        }
        if(queue_size<=5){
            c.add("iconst_" +Integer.toString(queue_size));
        }
        else{
            c.add("bipush " + Integer.toString(queue_size));
        }
        c.add("invokespecial " + type_actor +"/<init>(I)V");
        if(index_actor_in_main<=3){
            c.add("astore_" + Integer.toString(index_actor_in_main));
        }
        else{
            c.add("astore " + Integer.toString(index_actor_in_main));
        }
    }


    public void make_calling_set_known_actor_in_main(int index_actor_call , ArrayList<Identifier> known_actors ,
                                                     Hashtable<String,Integer> map , Identifier actor_call){

        ArrayList<String> c = this.commands.get("Main");
        if (index_actor_call<=3){
            c.add("aload_" + Integer.toString(index_actor_call));
        }
        else{
            c.add("aload " + Integer.toString(index_actor_call));
        }
        for (int i=0;i<known_actors.size();i++) {
            int index_known_actor = map.get(known_actors.get(i).getName());
            if (index_known_actor <= 3) {
                c.add("aload_" + Integer.toString(index_known_actor));
            } else {
                c.add("aload " + Integer.toString(index_known_actor));
            }
        }
        String types ="";
        for(int i=0 ;i<known_actors.size();i++){
            types+="L" + known_actors.get(i).getType() + ";";
        }
        c.add("invokevirtual " + actor_call.getType().toString()+"/setKnownActors(" +
                types+")V");
    }

    public void generate_code_start_for_actor_in_main(Identifier actor , int index){
        ArrayList<String> c = this.commands.get("Main");
        if(index<=3){
            c.add("aload_" + Integer.toString(index));
        }
        else{
            c.add("aload " + Integer.toString(index));
        }
        c.add("invokevirtual " + actor.getType().toString() +"/start()V");
    }

    public void end_main(){
        ArrayList<String> c = this.commands.get("Main");
        c.add("return");
        c.add(".end method");
    }


    public void create_msg_handler_in_actor_file(String actor_name , String msg_handler_name ,
                                                 int size_vars, ArrayList<VarDeclaration> args , boolean initial){
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("\n");
        if(initial){
            c.add(".method public " + msg_handler_name +"(" + convert_args_to_string(args) + ")V");
        }
        else{
            c.add(".method public " + msg_handler_name +"(LActor;" + convert_args_to_string(args) + ")V");
        }
        c.add(".limit stack 16");
        if (initial) {
            c.add(".limit locals " + Integer.toString(1 + size_vars));
        }
        else{
            c.add(".limit locals " + Integer.toString(2 + size_vars));
        }
    }

    public void end_msg_handler(String actor_name){
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("return");
        c.add(".end method");
    }

    public void create_a_label(String actor_name, String label_str, int lable_number){
        commands.get(actor_name).add(label_str+"_"+Integer.toString(lable_number)+":");
    }

    public void jump_to_label(String actor_name, String label_str, int lable_number){
        commands.get(actor_name).add("   goto "+label_str+"_"+Integer.toString(lable_number));
    }


    public void perform_math_operation(String actor_name ,BinaryOperator op ,boolean in_a_conditional ,
                                       Expression left , Expression right){
        if (op == BinaryOperator.add){
            commands.get(actor_name).add("iadd");
        }
        else if (op == BinaryOperator.sub){
            commands.get(actor_name).add("isub");
        }
        else if (op == BinaryOperator.mult){
            commands.get(actor_name).add("imul");
        }
        else if(op == BinaryOperator.div){
            commands.get(actor_name).add("idiv");
        }
        else if(op == BinaryOperator.and){
            commands.get(actor_name).add("iand");
        }
        else if(op == BinaryOperator.or){
            commands.get(actor_name).add("ior");
        }
        else if(op == BinaryOperator.mod){
            commands.get(actor_name).add("irem");
        }
        if(in_a_conditional == false){
            if(op == BinaryOperator.eq) {
                if (left.getType() instanceof IntType || left.getType() instanceof BooleanType) {
//                    int label1 = binary_number_jump_condition;
//                    binary_number_jump_condition++;
//                    int label2 = binary_number_jump_condition;
//                    binary_number_jump_condition++;
//                    commands.get(actor_name).add("if_icmpne lable" + Integer.toString(label1));
//                    commands.get(actor_name).add("iconst_1");
//                    commands.get(actor_name).add("goto lable" + Integer.toString(label2));
//                    commands.get(actor_name).add("lable" + Integer.toString(label1) + ":");
//                    commands.get(actor_name).add("iconst_0");
//                    commands.get(actor_name).add("lable" + Integer.toString(label2) + ":");
                }
                // string actor array
                else {
                    if (left.equals(right)) {
                        commands.get(actor_name).add("iconst_1");
                    } else {
                        commands.get(actor_name).add("iconst_0");
                    }
                }
            }

            else if (op == BinaryOperator.neq) {
//                int lable1 = binary_number_jump_condition;
//                binary_number_jump_condition++;
//                int lable2 = binary_number_jump_condition;
//                binary_number_jump_condition++;
//                if (left.getType() instanceof IntType || left.getType() instanceof BooleanType) {
//                    commands.get(actor_name).add("if_icmpeq lable" + Integer.toString(lable1));
//                    commands.get(actor_name).add("iconst_1");
//                    commands.get(actor_name).add("goto lable" + Integer.toString(lable2));
//                    commands.get(actor_name).add("lable" + Integer.toString(lable1) + ":");
//                    commands.get(actor_name).add("iconst_0");
//                    commands.get(actor_name).add("lable" + Integer.toString(lable2) + ":");
//                    this.binary_number_jump_condition += 1;
                } else {
                    if (left.equals(right)) {
                        commands.get(actor_name).add("iconst_0");
                    } else {
                        commands.get(actor_name).add("iconst_1");
                    }
                }
            }
            else if (op == BinaryOperator.lt){
//                int lable1 = binary_number_jump_condition;
//                binary_number_jump_condition++;
//                int lable2 = binary_number_jump_condition;
//                binary_number_jump_condition++;
//                commands.get(actor_name).add("if_icmpge lable"+Integer.toString(lable1));
//                commands.get(actor_name).add("iconst_1");
//                commands.get(actor_name).add("goto lable"+Integer.toString(lable2));
//                commands.get(actor_name).add("lable"+Integer.toString(lable1)+":");
//                commands.get(actor_name).add("iconst_0");
//                commands.get(actor_name).add("lable"+Integer.toString(lable2)+":");
            }
            else if (op == BinaryOperator.gt){
//                int lable1 = binary_number_jump_condition;
//                binary_number_jump_condition++;
//                int lable2 = binary_number_jump_condition;
//                binary_number_jump_condition++;
//                commands.get(actor_name).add("if_icmple lable"+Integer.toString(lable1));
//                commands.get(actor_name).add("iconst_1");
//                commands.get(actor_name).add("goto lable"+Integer.toString(lable2));
//                commands.get(actor_name).add("lable"+Integer.toString(lable1)+":");
//                commands.get(actor_name).add("iconst_0");
//                commands.get(actor_name).add("lable"+Integer.toString(lable2)+":");
            }
        }


    public void perform_unary_operation(String actor_name, UnaryOperator op , Expression exp ,
                                        boolean initial , boolean msg_handler,
                                        Hashtable<String,Integer> init_map , Hashtable<String,Integer> msg_map){
        if (op == UnaryOperator.not) {
//            int lable1 = binary_number_jump_condition;
//            binary_number_jump_condition += 1;
//            int lable2 = binary_number_jump_condition;
//            binary_number_jump_condition += 1;
//            commands.get(actor_name).add("if_icmpgt lable" + Integer.toString(lable1));
//            commands.get(actor_name).add("iconst_1");
//            commands.get(actor_name).add("jump to lable" + Integer.toString(lable2));
//            commands.get(actor_name).add("lable" + Integer.toString(lable1) + ":");
//            commands.get(actor_name).add("iconst_0");
//            commands.get(actor_name).add("lable" + Integer.toString(lable2) + ":");
        }
        else if (op == UnaryOperator.minus){
            commands.get(actor_name).add("ineg");
        }
        else if(op == UnaryOperator.postdec){


        }
        else if(op == UnaryOperator.postinc) {

        }
    }


    public void  add_command(String actor_name , String cmd){
        commands.get(actor_name).add(cmd);
    }

    public void add_command_for_boolean_value(String actor_name , BooleanValue value){
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("iconst_" + (value.getConstant() ? "1" : "0"));
    }

    public void add_command_for_int_value(String actor_name , IntValue value){
        ArrayList<String> c = this.commands.get(actor_name);
        c.add( (value.getConstant() > 5 ? "bipush " : "iconst_") +  Integer.toString(value.getConstant()));
    }

    public void add_command_for_string_value(String actor_name , StringValue value){
        ArrayList<String> c = this.commands.get(actor_name);
        c.add("ldc " + value.getConstant());
    }



    public void create_jump_with_condition(String actor_name, Expression condition, String end_label, int lable_number){
        if (condition.getClass().getName().toString().equals("ast.node.expression.BinaryExpression")) {
            BinaryExpression b = (BinaryExpression) condition;
            BinaryOperator op = b.getBinaryOperator();
            if (op == BinaryOperator.and || op == BinaryOperator.or) {
                commands.get(actor_name).add("   ifle "+end_label+"_"+Integer.toString(lable_number));
            }
            else if(op == BinaryOperator.eq){
                commands.get(actor_name).add("   if_icmpne "+end_label+"_"+Integer.toString(lable_number));
            }
            else if(op == BinaryOperator.neq){
                commands.get(actor_name).add("   if_icmpeq "+end_label+"_"+Integer.toString(lable_number));
            }
            else if(op == BinaryOperator.lt){
                commands.get(actor_name).add("   if_icmpge "+end_label+"_"+Integer.toString(lable_number));
            }
            else if(op == BinaryOperator.gt){
                commands.get(actor_name).add("   if_icmple "+end_label+"_"+Integer.toString(lable_number));
            }
        }
        else if(condition.getType().toString().equals("bool")){
            commands.get(actor_name).add("   ifle "+end_label+"_"+Integer.toString(lable_number));
        }

    }

}





