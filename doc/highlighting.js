"use strict";

(function() {
    var highlightRegexes = {
        "java": {
            //"block-comment": /\/\*[\s\S]*?!(\*\/)\*\//mg,
            "block-comment": /\/\*[\s\S]*?\*\//mg,
            "line-comment": /\/\/[^\n\r$]*[\n\r$]/g,
            "string": /"((\\")|([^"]))*"/g,
            "type": /\b((byte)|(short)|(int)|(long)|(float)|(double)|(char)|(boolean))\b/g,
            "keyword": /\b((abstract)|(assert)|(break)|(case)|(catch)|(class)|(const)|(continue)|(default)|(do)|(else)|(enum)|(extends)|(final)|(finally)|(for)|(goto)|(if)|(implements)|(import)|(instanceof)|(interface)|(native)|(new)|(package)|(private)|(protected)|(public)|(return)|(static)|(strictfp)|(super)|(switch)|(synchronized)|(this)|(throw)|(throws)|(transient)|(try)|(void)|(volatile)|(while))\b/g,
            "number": /\b[\+\-]?((\d+)|(\d+\.\d*)|(\d*\.\d+))([eE]([\+\-]?)\d+)?\b/g,
        },

        "jasm": {
            "line-comment": /#[^\n\r$]*[\n\r$]/g,
            "string": /"((\\")|([^"]))*"/g,
            "keyword": /(\.class|\.interface|\.enum|\.source|\.super|\.field|\.method|\.code|\.end code|\.table|\.end table)/g,
        },

        "pascal": {
            "block-comment": /\(\*([^*)]|\*[^)]|[^*]\))*\*\)/sg,
            "string": /'(('')|[^'])*'/g,
            "type": /\b(integer|string|text|array)\b/gi,
            "keyword": /\b(and|asm|begin|break|case|const|constructor|continue|destructor|div|do|downto|else|end|false|file|for|function|goto|if|implementation|in|inline|interface|label|mod|nil|not|object|of|on|operator|or|packed|procedure|program|record|repeat|set|shl|shr|string|then|to|true|type|unit|until|uses|var|var|while|with|xor)\b/gi,
            "number": /\b[\+\-]?((\d+)|(\d+\.\d*)|(\d*\.\d+))([eE]([\+\-]?)\d+)?\b/g,
        },
 
        "C": {
            //"block-comment": /\/\*[\s\S]*?!(\*\/)\*\//mg,
            "block-comment": /\/\*[\s\S]*?\*\//mg,
            "line-comment": /\/\/[^\n\r$]*([\n\r]|$)/g,
            "string": /"((\\")|([^"]))*"/g,
            "type": /\b((char)|(short)|(int)|(long)|(float)|(double)|(char)|(signed)|(unsigned))\b/g,
            "keyword": /\b(__attribute__|(break)|(case)|(const)|(continue)|(default)|(do)|(else)|(enum)|(for)|(goto)|(if)|inline|(return)|(sizeof)|(static)|struct|(switch)|(typedef)|(void)|(while))\b|#include|#define|#ifndef|#endif/g,
            "number": /\b[\+\-]?((\d+)|(\d+\.\d*)|(\d*\.\d+))([eE]([\+\-]?)\d+)?\b/g,
        },
 
        // Note: written offline without Python reference (incomplete!)
        "python": {
            "line-comment": /#[^\n\r$]*[\n\r$]/g,
            "string": /("((\\")|([^"]))*")|('((\\')|([^']))*')/g,
            "type": /\b((int)|(float)|(bool)|(str)|(list)|(set)|(dict))\b/g,
            "keyword": /\b((class)|(def)|(for)|(from)|(if)|(import)|(return)|(self))\b/g,
            "number": /\b[\+\-]?((\d+)|(\d+\.\d*)|(\d*\.\d+))([eE]([\+\-]?)\d+)?\b/g
        },
 
        "arm32": {
            "line-comment": /;[^\n\r$]*([\n\r]|$)/g,
            "number": /#((0[xX][0-9a-fA-F]+)|(\-?\d+))/g,
            "keyword": /\b(DCD|DCB|EXTERN|ALIGN|FILL|(MOV)|(CMP)|(B|BL)(|AL|GT|GE|LT|LE|EQ|NE)|LDR|(LSL)|(ADD|SUB|STOP|LSR|AND|SWI|TST|LDMFD|STMFD|STR|ORR)(|AL|GT|GE|LT|LE|EQ|NE)|BX)\b/ig,
            "string": /\b([Rr](0|1|2|3|4|5|6|7|8|9|(10)|(11)|(12)|(13)|(14)|(15)))|((SP)|(LR)|(PC))\b/ig,
            //"type": /\.[a-zA-Z0-9_]+\b/g,
        },
 
        "armv8": {
            "line-comment": /\/\/[^\n\r$]*[\n\r$]/g,
            "number": /#((0[xX][0-9a-fA-F]+)|(\-?\d+))/g,
            "keyword": /\b(LDUR|STUR|ADD|ADDI|ADDS|ADDIS|SUB|SUBI|SUBS|SUBIS|AND|ANDI|ANDS|ANDIS|ORR|ORRI|ORRS|ORRIS|EOR|EORI|EORS|EORIS|LSL|LSR|CBZ|CBNZ|B|B.EQ|B.NE|B.LT|B.GT|B.LE|B.GE|B.LO|B.LS|B.HI|B.HS|B.MI|B.PL|B.VS|B.VC)\b/g,
            //"string": /\b([Xx]([12]?[0-9])|30|31)\b/g
            //"string": /^[a-zA-Z0-9_]+\:/mg,
        },

        // Reference: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Lexical_grammar
        "javascript": {
            "block-comment": /\/\*[\s\S]*?\*\//mg,
            "line-comment": /\/\/[^\n\r$]*[\n\r$]/g,
            "string": /("((\\")|([^"]))*")|('((\\')|([^']))*')/g,
            "keyword": /\b(await|break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|false|finally|for|function|if|implements|import|in|instanceof|interface|let|new|null|package|private|protected|public|return|static|super|switch|this|throw|true|try|typeof|undefined|var|void|while|with|yield)\b/g,
            "number": /\b[\+\-]?((\d+)|(\d+\.\d*)|(\d*\.\d+))([eE]([\+\-]?)\d+)?\b/g,
        },
 
 
        // Incomplete:
        "SQL": {
            "line-comment": /--\s+[^\n\r$]*[\n\r$]/g,
            "number": /\b[\+\-]?((\d+)|(\d+\.\d*)|(\d*\.\d+))([eE]([\+\-]?)\d+)?\b/g,
            "keyword": /\b((ALL)|(AFTER)|(ALTER)|(AND)|(ANY)|(AS)|(ASC)|(AT)|(AUTO_INCREMENT)|(AVG)|(BEGIN)|(BETWEEN)|(BIGINT)|(BINARY)|(BLOB)|(BOOL)|(BOOLEAN)|(BY)|(BYTE)|(CASCADE)|(CASE)|(CHAR)|(CHARACTER)|(COALESCE)|(COLUMN)|(COLUMNS)|(COMMIT)|(CONSTRAINT)|(CONTAINS)|(DATE)|(DATETIME)|(DECIMAL)|(DEFAULT)|(DELETE)|(DESC)|(DESCRIBE)|(DROP)|(EXISTS)|(FOREIGN)|FROM|(IF)|(IN)|INNER|JOIN|(KEY)|(LIKE)|(NULL)|(ON)|(ORDER)|(SELECT)|(SET)|(TABLE)|(UNION)|UPDATE|(WHERE))\b/g,
            "string": /("((\\")|([^"]))*")|('((\\')|([^']))*')/g,
        },

        "make": {
            "line-comment": /#[^\n\r]*([\n\r]|$)/g,
        }
    };
    
    function highlight(region) {
        let lang = region.dataset.language;
        if (!lang) {
            console.warn("Assuming highlighting language 'java' (use data-language attribute to override)");
            lang = "java";
        }
        if (lang in highlightRegexes) {
            var regexes = highlightRegexes[lang];
            Object.keys(regexes).forEach(function(clazz) {
                highlightRegex(region, clazz, regexes[clazz]);
            });
        } else
            console.warn("Unknown highlight language: " + lang);
    }
    
    function highlightRegex(region, clazz, regex) {
        for (var i = 0; i < region.childNodes.length; ++i) {
            var node = region.childNodes[i];
            if (node.nodeType === HTMLElement.TEXT_NODE) {
                var text = node.textContent;
                var matches = matchRegexAll(text, regex);
                
                if (matches.length > 0) {
                    var nodesAdded = applyMatches(region, i, clazz, matches);
                    i += nodesAdded;
                }
            }
        }
    }
    
    function matchRegexAll(text, regex) {
        var matches = [];
        var m;
        do {
            m = regex.exec(text);
            if (m)
                matches.push([m.index, m[0].length]);
        } while (m);
        
        return matches;
    }
    
    function applyMatches(region, textNodeIndex, clazz, matches) {
        var lastMatch = matches[matches.length - 1];
        var matchIndex = lastMatch[0],
            matchLength = lastMatch[1];
        var newNodes = 0;
        var span = document.createElement("span");
        span.classList.add(clazz);
        var textNode = region.childNodes[textNodeIndex];
        span.innerText = textNode.textContent.substring(matchIndex, matchIndex + matchLength);
        region.insertBefore(span, region.childNodes[textNodeIndex + 1]);
        ++newNodes;
        
        // Is there any text after this match that we need to preserve?
        if (textNode.textContent.length > matchIndex + matchLength) {
            var endingText = textNode.textContent.substring(matchIndex + matchLength);
            var newTextNode = document.createTextNode(endingText);
            region.insertBefore(newTextNode, region.childNodes[textNodeIndex + 2]);
            ++newNodes;
        }
        
        textNode.textContent = textNode.textContent.substring(0, matchIndex);
        
        if (matches.length > 1)
            return newNodes + applyMatches(region, textNodeIndex, clazz, matches.slice(0, -1));        
        else
            return newNodes;
    }
    
    var highlightRegions = document.querySelectorAll(".highlight");
    highlightRegions.forEach(function(r) { highlight(r); });
})();
