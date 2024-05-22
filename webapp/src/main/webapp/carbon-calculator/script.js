const questionnaireData = [
    {
        section: "food",
        logo: "./assets/fast-food.png",
        title: "Food",
        questions: [
            {
                question: "How would you best describe your diet?",
                answers: [
                   {text: "Meat in every meal", carbon: 10},
                    {text:"Meat in some meals", carbon: 8},
                   {text:"No beef", carbon: 5},
                  {text:"Meat very rarely", carbon: 3 },
                   {text:"Vegetarian", carbon: 2},
                   {text: "Vegan", carbon: 1}
               ],
                nextSection: "travel"
            },
         {
                question: "In a week, how much do you spend on food from restaurants, canteens and takeaways?",
                answers: [
                  {text: "0€", carbon: 1},
                    {text:"1€ - 10€", carbon: 2},
                    {text:"10€ - 60€", carbon: 3},
                    {text:"More than 60€", carbon: 4},
                ],
                nextSection: "travel"
            }, 
          {
                question: "Of the food you buy how much is wasted and thrown away?",
                answers: [
                    {text:"None", carbon: 0},
                    {text:"0% - 10%", carbon: 1},
                    {text:"10% - 30%", carbon: 2},
                    {text:"More than 30%", carbon:3}
                ],
                nextSection:"travel"
            },
         {
                question:"How often do you buy locally produced food that is not imported? ",
                answers: [
                    {text:"A lot of the food I buy is locally sourced", carbon: 1 },
                    {text:"Some of the food I buy is locally sourced", carbon: 2 },
                    {text:"I don't worry about where my food comes from", carbon: 3}
                ],
                nextSection:"travel"
            }
        ]
    },
    {
        section: "travel",
        logo: "../assets/carros.png",
        title: "Travel",
        questions: [
            {
                question:"What kind of vehicle do you travel in most often as driver or passenger?",
                answers: [
                    {text: "Car",carbon: 10},
                    {text:"Motorbike",carbon: 5},
                    {text: "Neither - I walk, cycle or use public transport for all my journeys" ,carbon: 0}
                ],
                nextSection: "home"
          },
         {
                question: "Which of these best describes the vehicle you use most?",
                answers: [
                    {text: "Electric car",carbon: 2},
                    {text:"Hybrid car",carbon: 4},
                    {text: "Small petrol or diesel car",carbon: 6},
                    {text: "Medium petrol or diesel car",carbon: 8},
                    {text: "Large pretrol or diesel car",carbon: 10},
                ],
                nextSection: "home"
          },
         {
                question: "How many hours a week do you spend in your car or on your motorbike for personal use including commuting?",
                answers: [
                    {text:  "Under 2 hours",carbon: 2},
                    {text: "2 to 5 hours",carbon: 3},
                    {text: "5 to 15 hours",carbon: 4},
                    {text:  "15 to 25 hours",carbon: 5},
                    {text: "Over 25 hours",carbon: 6},
                ],
                nextSection:"home"
          },
         {
                question: "How many hours a week do you spend on the train for personal use including commuting?",
                answers: [
                    {text: "I don't travel by train",carbon: 10},
                    {text: "Under 2 hours",carbon: 1},
                    {text:  "2 to 5 hours",carbon: 2},
                    {text:  "5 to 15 hours",carbon: 3},
                    {text:  "15 to 25 hours",carbon: 4},
                    {text:  "Over 25 hours",carbon: 5},
                ],
                nextSection: "home"
          },
         {
                question: "How many hours a week do you spend on the bus for personal use including commuting?",
                answers: [
                    {text:  "I don't travel by bus",carbon: 10},
                    {text: "Under 1 hour",carbon: 1},
                    {text: "1 to 3 hours",carbon: 2},
                    {text:  "3 to 6 hours",carbon: 3},
                    {text: "6 to 10 hours",carbon: 4},
                    {text:   "Over 10 hours",carbon: 5},
                ],
                nextSection: "home"
          },
         {
                question: "In the last year, how many flights have you made in total?",
                answers: [
                    {text:   "I didn't fly anywhere",carbon: 0},
                    {text:   "1-2 flights",carbon: 5},
                    {text:   "2-5 flights",carbon: 10},
                    {text:   "Over 5 flights",carbon: 15},
                ],
                nextSection: "home"
          },
         {
                question: "What percentage of your flights do you offset?",
                answers: [
                    {text:    "None of them",carbon: 10},
                    {text:    "25%",carbon: 5},
                    {text:  "50%",carbon: 4},
                    {text: "75%",carbon: 3},
                    {text:  "All of them",carbon: 2},
                    {text:    "Not applicable",carbon: 0},
                ],
                nextSection: "home"
          }
        ]
    },
    {
        section: "home",
       logo: "../assets/botao-de-inicio.png",
        title: "Home",
       questions: [
            {
                question: "What kind of house do you live in?",
                answers: [
                    {text:     "Detached",carbon: 10},
                    {text:  "Semi detached",carbon: 8},
                    {text:    "Terrace",carbon: 6},
                    {text:  "Flat",carbon: 4},
               ],
                nextSection: "Stuff"
           },
         {
                question: "How many bedrooms does your house have?",
                answers: [
                    {text:      "1",carbon: 3},
                    {text:    "2",carbon: 4},
                    {text:  "3",carbon: 7},
                    {text:   "4 or more",carbon: 10},
                ],
                nextSection: "Stuff"
           },
         {
                question: "How many people (aged 17 and over) live in your house?",
                answers: [
                    {text:     "1",carbon: 2},
                    {text:  "2",carbon: 3},
                    {text:  "3",carbon: 4},
                    {text:  "4",carbon: 5},
                    {text:   "5 or more",carbon: 8},
               ],
                nextSection: "Stuff"
           },
         {
                question: "How do you heat your home?",
                answers: [
                    {text:     "Gas",carbon: 6},
                    {text: "Oil",carbon: 10},
                    {text: "Electricity",carbon: 2},
                    {text:     "Wood",carbon: 2},
                    {text:   "Heatpump", carbon: 2},
              ],
                nextSection: "Stuff"
           },
         {
                question: "Does your electricity come from renewable generation?",
                answers: [
                    {text:    "I don't know",carbon: 5},
                    {text:   "No",carbon: 10},
                    {text:"Yes, but the source is not 100% renewable",carbon: 6},
                    {text:  "Yes 100%",carbon: 0},
              ],
                nextSection: "Stuff"
           },
         {
                question: "Do you regularly turn off lights and not leave your appliances on standby?",
                answers: [
                    {text:   "Yes",carbon: 0},
                    {text:     "No",carbon: 5},
                ],
                nextSection: "Stuff"
           },
         {
                question: "How warm do you keep your home in winter?",
                answers: [
                    {text:    "Below 14 degrees",carbon: 1},
                    {text:   "14 - 17 degrees",carbon: 2},
                    {text:  "18 - 21 degrees",carbon: 3},
                    {text:   "Over 21 degrees",carbon: 5},
                ],
                nextSection: "Stuff"
           },
         {
                question: "How many home energy efficiency improvements are installed in your home?",
                answers: [
                    {text:   "0",carbon: 10},
                    {text: "1",carbon: 8},
                    {text: "2",carbon: 7},
                    {text: "2-4",carbon: 5},
                    {text: "4-8",carbon: 2},
                    {text: "8 or more",carbon: 0},
                ],
                nextSection: "Stuff"
           }
        ]
    }, 
    {
        section: "Stuff",
        logo: "../assets/carrinho.png",
        title: "Stuff",
        questions: [
            {
                question: "In the last 12 months, have you bought any of these new household items?",
                answers: [
                    {text:   "TV, laptop or PC",carbon: 3},
                    {text: "Large item of furniture",carbon: 3},
                    {text:  "Washing machine, dishwasher, tumble dryer or fridge freezer",carbon: 5},
                    {text: "Mobile phone or tablet",carbon: 2},
                    {text: "Nothing", carbon: 0}
                ],
                nextSection: "result"
            },
         {
                question: "In a typical month, how much do you spend on clothes and footwear?",
                answers: [
                    {text:  "0€",carbon: 0},
                    {text:"1€ - 60€",carbon: 2},
                    {text: "60€ - 180€",carbon: 5},
                    {text:   "180€ or more",carbon: 9},
                ],
                nextSection: "result"
            },
         {
                question: "In a typical month, how much do you spend on your pets and pet food?",
                answers: [
                    {text:  "I don't have a pet",carbon: 0},
                    {text: "1€ - 10€",carbon: 2},
                    {text: "10€ - 35€",carbon: 4},
                    {text: "35€ or more",carbon: 6},
                ],
                nextSection: "result"
            },
         {
                question: "In a typical month, how much do you spend on health, beauty and grooming products?",
                answers: [
                    {text:   "0€ - 10€",carbon: 1},
                    {text: "10€ - 60€",carbon: 4},
                    {text: "60€ or more",carbon: 5},
                ],
                nextSection: "result"
            },
         {
                question: "In a typical month, how much do you spend on phone, internet and TV contracts?",
                answers: [
                    {text:   "0€",carbon: 0},
                    {text:"1€ - 35€",carbon: 3},
                    {text: "35€ - 75€",carbon: 6},
                    {text: "70€ or more",carbon: 9},
                ],
                nextSection: "result"
            },
         {
                question: "In a typical month, how much do you spend on entertainment and hobbies (sports/gym, cinema, books, newspapers, gardening, computer games)",
                answers: [
                    {text:   "0€ - 25€",carbon: 0},
                    {text: "25€ - 50€",carbon: 3},
                    {text:  "50€ - 75€",carbon: 6},
                    {text: "75€ or more",carbon: 9},
                ],
                nextSection: "result"
            },
         {
                question: "How many types of waste do you recycle and/or compost?",
                answers: [
                    {text:    "0",carbon: 10},
                    {text: "1",carbon: 8},
                    {text:  "2",carbon: 6},
                    {text:"3",carbon: 4},
                    {text: "4",carbon: 0},
               ],
                nextSection: "result"
            }

     ]
    }
    
];

const questionnaire = document.getElementById("questionnaire");
let currentSection = "food";
let currentQuestionIndex = 0;

function renderQuestionnaire() {
    const section = questionnaireData.find(
        (sectionObj) => sectionObj.section === currentSection
    );

    if (!section) {
        return;
    }

    const question = section.questions[currentQuestionIndex];

    if (!question) {
        return;
    }

    const progress = ((currentQuestionIndex + 1) / section.questions.length) * 100;

    

    const questionHTML = `
    <div class="calculator">
        <div class="header">
         <div class="subheader">
                <img src="${section.logo}" alt="${section.section} Logo" class="section-logo">
                <h2>${section.title}</h2>
                <p class="question-counter">Q${currentQuestionIndex + 1} of ${section.questions.length}</p>
            </div>
            <div class="progress-bar">
                <div class="progress" style="width: ${progress}%"></div>
         </div>
        </div>

        <div class="question">
           <p class="question-text">${question.question}</p>
            <div class="answers">
              ${question.answers.map((answer) => 
                    `<button class="button">${answer.text}</button>`).join("")}
            </div>
            <div class="navigation">
                <button id="back-btn" class="btn" ${currentQuestionIndex === 0 && section.title === "Food" ? 'disabled' : ''}>Back</button>
                <button id="next-btn" class="btn">Next Question</button>
            </div>
        </div>
    </div>
    `;

    questionnaire.innerHTML = questionHTML;

    document.getElementById("next-btn").addEventListener("click", goToNextQuestion); //Back button
    document.getElementById("back-btn").addEventListener("click", goToPreviousQuestion);//Next question button
}


function goToPreviousQuestion() {

    if (currentQuestionIndex > 0) {
        currentQuestionIndex--;
       
    } else {
        const previousSection = getPreviousSection();
        if (previousSection) {
            currentSection = previousSection;
            const previousSectionQuestions = questionnaireData.find((sectionObj) => sectionObj.section === currentSection).questions;
            currentQuestionIndex = previousSectionQuestions.length - 1;
        } else {
            // No previous section, already at the first question
            return;
        }
    }
    renderQuestionnaire();
}

function goToNextQuestion() {
    const section = questionnaireData.find((sectionObj) => sectionObj.section === currentSection);
    
    if (!section) {
        return;
    }

    if (currentQuestionIndex < section.questions.length - 1) {
        currentQuestionIndex++;
    } else {
        const nextSection = getNextSection();
        if (nextSection) {
            currentSection = nextSection;
            currentQuestionIndex = 0;
        } else {
            // No next section, end of questionnaire
            return;
        }
    }

    renderQuestionnaire();
}

let userAnswers = [];

function handleAnswerClick(e) {
    if (!e.target.classList.contains("button")) 
        return;
    

    const answer = e.target.textContent;
    const section = questionnaireData.find((sectionObj) => sectionObj.section === currentSection);
    const question = section.questions[currentQuestionIndex];

    // Find the selected answer and its carbon footprint
    const selectedAnswer = question.answers.find((ans) => ans.text === answer);
    const carbonValue = selectedAnswer.carbon;

    // Store the user's answer and its carbon footprint
    userAnswers.push({ question: question.question, answer: selectedAnswer, carbon: carbonValue});

    if (currentQuestionIndex < section.questions.length - 1) {
        currentQuestionIndex++;
    } else {
        currentSection = getNextSection();
        currentQuestionIndex = 0;
    }
    calculateCarbonFootprint();
    renderQuestionnaire();
}

const medianCarbonFootprint = 109; //Change after (test value)

function calculateCarbonFootprint() {
    let totalCarbonFootprint = 0;

    userAnswers.forEach(answer => {
        totalCarbonFootprint += answer.carbon;
    });
    console.log(totalCarbonFootprint);
    return totalCarbonFootprint;
}

function provideFeedback(userCarbonFootprint) {
    const carbonFootprintPercentage = (userCarbonFootprint / medianCarbonFootprint) * 100;

    let feedback = "Your carbon footprint is " + carbonFootprintPercentage.toFixed(2) + "%.\n";

    if (carbonFootprintPercentage > 100) {
        feedback += "You are emitting more carbon then the average person. Consider these tips to reduce your footprint:\n";
        //ADD TIPS HERE
        //probably a switch would be the option :=

    } else {
        feedback += "You are emitting less carbon then the average person, congratz!\n";
    }
    
    return feedback;
}

function renderFeedback(feedbackMessage) {
    const feedbackSection = document.getElementById("feedback-section");
    const feedbackMessageElement = document.getElementById("feedback-message");

    feedbackMessageElement.textContent = feedbackMessage;
    feedbackSection.classList.add("fade-in");
    feedbackSection.style.display = "block";
}

function fadeOutElement(element) {
    element.classList.add("fade-out");
    setTimeout(() => {
        element.style.display = "none";
    }, 500);
}

function getNextSection() {
    const section = questionnaireData.find(
        (sectionObj) => sectionObj.section === currentSection
    );

    if (!section) {
        return null;
    }

    if (currentQuestionIndex < section.questions.length - 1) {
        return currentSection;
    }

    const nextSection = section.questions[currentQuestionIndex].nextSection;

    if (nextSection === "result") {
        const feedback = provideFeedback(calculateCarbonFootprint());
        const questionnaireElement = document.getElementById("questionnaire");
        const questionnaireTitle = document.getElementById("title");
        if (questionnaireElement) {
            fadeOutElement(questionnaireElement);
            fadeOutElement(questionnaireTitle);
            setTimeout(() => {
                renderFeedback(feedback);
            }, 500);
        } else {
            console.log("Questionnaire not found!")
        }
        return null;
    }

    return nextSection;
}

function getPreviousSection() {
    const sectionIndex = questionnaireData.findIndex((sectionObj) => sectionObj.section === currentSection);
    if (sectionIndex > 0) {
        return questionnaireData[sectionIndex - 1].section;
    }
    return null;
}

questionnaire.addEventListener("click", handleAnswerClick);
renderQuestionnaire();
