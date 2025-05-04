"use strict";

(function() {
    function isPlainMode() {
        if (window.location.search) {
            const params = new URLSearchParams(window.location.search);
            if (params.has("plain"))
                return true;
        }
        
        return false;
    }
    
    function slugify(s) {
        let slug = "";
        for (let i = 0; i < s.length; ++i) {
            const c = s[i];
            if ("A" <= c && c <= "Z"
                    || "a" <= c && c <= "z"
                    || "0" <= c && c <= "9"
                    || c === "_" || c === "-")
                slug += c;
            else
                slug += "_";
        }
        
        return slug;
    }
    
    const plain_mode = isPlainMode();
    
    if (plain_mode)
        document.querySelector("html").classList.remove("fancy");
    
    // Insert an anchor before each problem, and if there is a problem links ul,
    // add a link to it
    const problem_titles = document.querySelectorAll(".problems > .problem > .title");
    const nav_ul = document.querySelector("ul.problem-links");
    let problem_number = 0;
    for (let i = 0; i < problem_titles.length; ++i) {
        const problem_title = problem_titles[i];
        let link_text = undefined;
        let anchor_name = undefined;
        
        if (problem_title.classList.contains("auto-number")) {
            ++problem_number;
            link_text = "Problem " + problem_number;
            anchor_name = "p" + problem_number;
            problem_title.innerText = "" + problem_number;
        } else {
            link_text = problem_title.innerText.trim() || undefined;
            anchor_name = slugify(link_text);
        }
        
        if (link_text && anchor_name) {
            const anchor = document.createElement("a");
            anchor.setAttribute("name", anchor_name);
            problem_title.parentElement.parentElement.insertBefore(anchor, problem_title.parentElement);
        
            if (nav_ul) {
                const li = document.createElement("li");
                const link = document.createElement("a");
                link.setAttribute("href", "#" + anchor_name);
                link.setAttribute("target", "_self");
                const text = document.createTextNode(link_text);
                link.appendChild(text);
                li.appendChild(link);
                nav_ul.appendChild(li);
            }
        }
    }
    
    // Watch document scrolls and highlight the link to the current
    // problem
    const problems = document.querySelectorAll(".problems > .problem");
    
    // Since some problems don't have links, we need to keep track of the mapping between problem index and link index!
    const problem_link_index_map = [ 0 ];
    
    let link_index = 1;
    
    for (let problem of document.querySelectorAll(".problems > .problem")) {
        problem_link_index_map.push(link_index);
        if (problem.querySelector(".title"))
            ++link_index;
    }
    
    let current_tab = null;
    let current_problem = -1;
    
    function updateCurrent() {
        const viewportHeight = document.querySelector("html").clientHeight;
        let new_current_problem = -1;
        for (let i = problems.length - 1; i >= 0; --i) {
            const client_rect = problems[i].getBoundingClientRect();
            if (client_rect.top < viewportHeight * 0.3) {
                new_current_problem = i;
                break;
            }
        }
        
        if (new_current_problem === -1 && problems.length > 0)
            new_current_problem = 0;
        
        const tabs = document.querySelectorAll("nav ul li");
        if (new_current_problem !== current_problem) {
            if (current_tab !== null)
                current_tab.classList.remove("current");
            if (new_current_problem !== -1) {
                let tab_index = problem_link_index_map[new_current_problem];
                if (tab_index >= tabs.length) {
                    tab_index = tabs.length - 1;
                }
                current_tab = tabs[tab_index];
                current_tab.classList.add("current");
            } else
                current_tab = null;
            
            current_problem = new_current_problem;
        }
    }
    
    if (nav_ul) {
        document.addEventListener("scroll", updateCurrent);
        updateCurrent();
    }
    
    for (let problem of problems) {
        const total_points = problem.querySelector(".total.points");
        const points = problem.querySelectorAll(".points:not(.total)");
        
        if (!total_points)
            continue;
        
        let sum = 0;
        for (let point of points) {
            const value = parseInt(point.innerText);
            if (!isNaN(value)) {
                sum += value;
                if (!point.hasAttribute("title"))
                    point.setAttribute("title", value + (value === 1 ? " point" : " points"));
            }
        }
        
        if (total_points && total_points.innerText.trim() === "")
            total_points.innerText = sum + (sum === 1 ? " point" : " points");
    }

    
    const homework_total_points_box = document.querySelector(".assignment-info .total.points");
    if (homework_total_points_box) {
        let homework_total_points = 0;
        const points_pattern = /^\s*(\d+)\s+points?\s*$/i;
        for (let problem_total_points of document.querySelectorAll(".problem .total.points")) {
            const match = points_pattern.exec(problem_total_points.innerText);
            if (match)
                homework_total_points += parseInt(match[1]);
        }
        
        homework_total_points_box.innerText = homework_total_points + (homework_total_points === 1 ? " point" : " points");
    }

    if (plain_mode) {
        let problem_counter = 1;
        for (let problem_title of document.querySelectorAll(".problems > .problem > .title")) {
            if (problem_title.classList.contains("auto-number")) {
                problem_title.innerText = "Problem " + problem_counter;
                ++problem_counter;
            }
        }
        
        for (let point of document.querySelectorAll(".problem .points:not(.total)")) {
            point.innerText = "(" + point.innerText + " points)";
        }
    }

    const darkModeToggle = document.querySelector(".dark-mode-toggle");
    const html = document.querySelector("html");
    if (darkModeToggle) {
        darkModeToggle.addEventListener("click", () => {
            html.classList.toggle("dark-mode");
            localStorage.setItem("dark-mode", html.classList.contains("dark-mode") ? "yes" : "");
        });
    }

    if (localStorage.getItem("dark-mode"))
        document.querySelector("html").classList.add("dark-mode");
})();
