# AI-Budgerigar

## Project Overview: Canvas Intelligent Plugin
The Canvas Intelligent Plugin project focuses on implementing key AI-driven features within  Canvas LMS to enhance the learning experience for students and streamline course management for teachdrs. This includes a smart chatbot that provides students with instant homework assistance, a quiz function to ask topic-related questions to help self-assess knowledge, and tracks their learning progress. The system offers immediate feedback on quizzes and adjusts question difficulty based on student performance. For instructors, the plugin simplifies course material management by enabling them to upload and delete learning resources, while also analysing frequently asked student questions to help address common areas of confusion. These core features aim to improve student outcomes and reduce the workload on teachers.

## Project Goal
- Enhance student learning by providing instant homework assistance, adaptive quizzes, and personalised progress tracking.
- Streamline course management for instructors, simplifying material management and analysing student inquiries to reduce workload and improve teaching effectiveness.

## Roles and Responsibilities
### Product Owner
- Define and prioritise the product backlog to align with project goals.
- Act as the primary liaison between stakeholders and the development team.
- Clearly communicate the product vision and objectives to the team.
- Make decisions on scope, release plans, and feature prioritisation.
- Review and accept completed work to ensure it meets the project requirements.

### Scrum Master
- Facilitate Scrum ceremonies, including sprint planning, daily stand-ups, sprint reviews, and retrospectives.
- Remove obstacles that impede the team's progress.
- Ensure the team adheres to Agile principles and practices.
- Promote continuous improvement within the team.
- Foster a collaborative and productive team environment.

### Development Environment Lead
- Set up and maintain the development environment, including tools, and software.
- Ensure the environment is stable, scalable, and accessible to all team members.
- Monitor and optimise the performance of the development environment.
- Provide technical support to the development team for environment-related issues.
- Keep the environment up to date with the latest updates, and security measures.

### Quality Assurance (QA) Lead
- Develop and implement a comprehensive QA strategy and test plans.
- Oversee all testing activities, including unit tests, integration tests, system tests, and user acceptance tests.
- Ensure the product meets the required quality standards before release.
- Identify, document, and track defects; work with the development team to resolve them.
- Continuously improve QA processes and methodologies.

### Architecture Lead
- Design and oversee the overall system architecture, ensuring it meets technical and business requirements.
- Review and approve architectural changes and technical decisions.
- Provide technical guidance to the team.
- Ensure the architecture is scalable, secure, and maintainable.
 
## Meet The Team
| **Name**              | **Role**                     | **Contact**                         |
| -----------           | -----------                  | -----------                         |
| Shrimant Kohli        | Product Owner                | sskohli@student.unimelb.edu.au      |
| Adrian Dam            | Scrum Master                 | addam@student.unimelb.edu.au        |
| Weiran Xu             | Development Environment Lead | weiranx1@student.unimelb.edu.au     |
| Yongchun Li           | Architecture Lead            | yongchunl@student.unimelb.edu.au    |
| Xiang Wang            | Developer                    | xiang.wang2411@gmail.com            |
| Pengyuan Yu           | Developer                    | pengyuany@student.unimelb.edu.au    |
| Rong Wang             | Developer                    | rongwang@student.unimelb.edu.au     |
| Yuxin Ren             | Quality Assurance Lead       | yuxinr1@student.unimelb.edu.au      |



```
|   Dockerfile
|   README.md
|   
+---demo
|   |   mvnw
|   |   mvnw.cmd
|   |   pom.xml
|   |   README.md
|   |   
|   +---.mvn
|   |           
|   \---src
|       +---main
|       |   +---java
|       |   |   \---com
|       |   |       \---AI
|       |   |           \---Budgerigar
|       |   |               \---chatbot
|       |   |                   |   ChatBotApplication.java
|       |   |                   |   
|       |   |                   +---AIUtil
|       |   |                   |       Message.java
|       |   |                   |       
|       |   |                   +---Config
|       |   |                   |       AppConfig.java
|       |   |                   |       ArkServiceConfig.java
|       |   |                   |       BaiduConfig.java
|       |   |                   |       OpenAIRestTemplateConfig.java
|       |   |                   |       WebConfig.java
|       |   |                   |       
|       |   |                   +---Controller
|       |   |                   |       ChatController.java
|       |   |                   |       
|       |   |                   +---DTO
|       |   |                   |       ChatRequest.java
|       |   |                   |       ChatResponse.java
|       |   |                   |       ErrorResponse.java
|       |   |                   |       
|       |   |                   \---Services
|       |   |                       |   ChatService.java
|       |   |                       |   
|       |   |                       \---impl
|       |   |                               BaiduChatServiceImpl.java
|       |   |                               DouBaoChatServiceImpl.java
|       |   |                               OpenAIChatServiceImpl.java
|       |   |                               
|       |   \---resources
|       |           application.properties
|       |           
|       \---test
|           \---java
|               \---com
|                   \---AI
|                       \---Budgerigar
|                           \---chatbot
|                                   DemoApplicationTests.java
|                                   
+---docs
|   |   executable_architecture.pdf
|   |   risk_tracking_table.md
|   |   
|   +---design concept
|   |       component_diagram.png
|   |       design_concept.md
|   |       
|   \---meetings
|       |   meeting_minutes_template.md
|       |   
|       \---sprint one
|               sprint_planning.md
|               sprint_standups.md
|               sprint_retro.md
|               
\---gpt-clone
```
