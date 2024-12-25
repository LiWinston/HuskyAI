// 常用编程语言的Hello World示例
export const CODE_EXAMPLES = {
    javascript: {
        name: 'JavaScript',
        code: `function greet() {
    console.log("Hello, World!");
}

greet();`
    },
    python: {
        name: 'Python',
        code: `def greet():
    print("Hello, World!")

if __name__ == "__main__":
    greet()`
    },
    java: {
        name: 'Java',
        code: `public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}`
    },
    cpp: {
        name: 'C++',
        code: `#include <iostream>

int main() {
    std::cout << "Hello, World!" << std::endl;
    return 0;
}`
    },
    csharp: {
        name: 'C#',
        code: `using System;

class Program {
    static void Main() {
        Console.WriteLine("Hello, World!");
    }
}`
    },
    rust: {
        name: 'Rust',
        code: `fn main() {
    println!("Hello, World!");
}`
    },
    go: {
        name: 'Go',
        code: `package main

import "fmt"

func main() {
    fmt.Println("Hello, World!")
}`
    },
    ruby: {
        name: 'Ruby',
        code: `def greet
    puts "Hello, World!"
end

greet`
    },
    php: {
        name: 'PHP',
        code: `<?php
function greet() {
    echo "Hello, World!";
}

greet();
?>`
    },
    swift: {
        name: 'Swift',
        code: `func greet() {
    print("Hello, World!")
}

greet()`
    },
    kotlin: {
        name: 'Kotlin',
        code: `fun main() {
    println("Hello, World!")
}`
    },
    typescript: {
        name: 'TypeScript',
        code: `function greet(): void {
    console.log("Hello, World!");
}

greet();`
    },
    sql: {
        name: 'SQL',
        code: `SELECT 'Hello, World!' AS greeting;`
    },
    html: {
        name: 'HTML',
        code: `<!DOCTYPE html>
<html>
<head>
    <title>Hello World</title>
</head>
<body>
    <h1>Hello, World!</h1>
</body>
</html>`
    },
    css: {
        name: 'CSS',
        code: `body {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    margin: 0;
    font-family: sans-serif;
}

h1::before {
    content: "Hello, World!";
}`
    }
}; 