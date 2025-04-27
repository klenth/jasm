package edu.westminsteru.cmpt355.jasm;

public sealed interface CodeItem
    permits Instruction, Table
{ }
