package edu.westminsteru.jasm;

sealed interface CodeItem
    permits Instruction, Table
{ }
