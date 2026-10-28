/* =====================================================================
 * chat.js — Intelligent D2 Analytics AI Assistant
 * Provides both:
 * 1) Full-page interactive chat interface (page-chat)
 * 2) Global floating chat widget (#floatingChatWidget)
 * ===================================================================== */

var chatHistory = [];
var isChatWaiting = false;

// Initialize on DOM load
window.addEventListener("DOMContentLoaded", () => {
    loadChatSuggestions();
});

function loadChatSuggestions() {
    fetch("/api/chat/suggestions")
        .then(r => r.json())
        .then(suggestions => {
            renderSuggestionChips(suggestions, "fullChatChips");
            renderSuggestionChips(suggestions, "floatChatChips");
        })
        .catch(() => {});
}

function renderSuggestionChips(chips, containerId) {
    const el = document.getElementById(containerId);
    if (!el || !chips) return;
    el.innerHTML = chips.map(chip => 
        `<button class="chat-chip-btn" onclick="sendSuggestion('${escapeHtml(chip)}')">${escapeHtml(chip)}</button>`
    ).join("");
}

function sendSuggestion(text) {
    const fullInput = document.getElementById("fullChatInput");
    const floatInput = document.getElementById("floatChatInput");
    if (fullInput && document.getElementById("page-chat").classList.contains("active")) {
        fullInput.value = text;
        submitChat("full");
    } else if (floatInput) {
        floatInput.value = text;
        submitChat("float");
    } else if (fullInput) {
        fullInput.value = text;
        submitChat("full");
    }
}

function submitChat(mode) {
    if (isChatWaiting) return;

    const inputEl = document.getElementById(mode === "float" ? "floatChatInput" : "fullChatInput");
    if (!inputEl) return;

    const text = inputEl.value.trim();
    if (!text) return;

    inputEl.value = "";
    isChatWaiting = true;

    // Append user message
    appendMessage("user", text);

    // Show typing indicator
    showTypingIndicator();

    // Prepare payload
    const payload = {
        message: text,
        history: chatHistory.slice(-6)
    };

    fetch("/api/chat/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    })
    .then(res => {
        if (!res.ok) throw new Error("Chat request failed");
        return res.json();
    })
    .then(data => {
        removeTypingIndicator();
        appendMessage("bot", data.reply, data.actionType, data.actionPayload);
        if (data.quickSuggestions && data.quickSuggestions.length > 0) {
            renderSuggestionChips(data.quickSuggestions, "fullChatChips");
            renderSuggestionChips(data.quickSuggestions, "floatChatChips");
        }
    })
    .catch(err => {
        removeTypingIndicator();
        appendMessage("bot", "⚠️ *Sorry, I encountered an issue processing your request. Please try again.*");
        console.error("Chat error:", err);
    })
    .finally(() => {
        isChatWaiting = false;
        if (inputEl) inputEl.focus();
    });
}

function appendMessage(sender, text, actionType, actionPayload) {
    chatHistory.push({ sender, text });

    const formattedHtml = parseMarkdown(text);

    let actionBtnHtml = "";
    if (actionType && actionPayload) {
        if (actionType === "SWITCH_TAB") {
            const label = actionPayload === "growth" ? "📈 Open Growth Analytics" : 
                          actionPayload === "compare" ? "⚖️ Open Site Compare" : "📊 Open Chart View";
            actionBtnHtml = `<div style="margin-top:10px;">
                <button class="chat-action-btn" onclick="executeChatAction('${actionType}', '${actionPayload}')">
                    ${label}
                </button>
            </div>`;
        } else if (actionType === "SELECT_SITE") {
            actionBtnHtml = `<div style="margin-top:10px;">
                <button class="chat-action-btn" onclick="executeChatAction('${actionType}', '${actionPayload}')">
                    📊 View ${actionPayload} on Dashboard
                </button>
            </div>`;
        } else if (actionType === "EXPORT_REPORT") {
            actionBtnHtml = `<div style="margin-top:10px;">
                <button class="chat-action-btn" onclick="executeChatAction('${actionType}', '${actionPayload}')">
                    📥 Download Executive PDF Report
                </button>
            </div>`;
        }
    }

    const botToolsHtml = sender === "bot" ? `
        <div style="display:flex;gap:8px;margin-top:6px;align-items:center;">
            <button class="chat-tool-btn" onclick="copyChatBubble(this)" title="Copy answer" style="background:transparent;border:none;color:var(--muted);cursor:pointer;font-size:11px;padding:2px 5px;border-radius:4px;">📋 Copy</button>
            <button class="chat-tool-btn" onclick="speakChatBubble(this)" title="Listen" style="background:transparent;border:none;color:var(--muted);cursor:pointer;font-size:11px;padding:2px 5px;border-radius:4px;">🔊 Listen</button>
        </div>` : "";

    const msgHtml = `
        <div class="chat-bubble ${sender}">
            <div class="chat-avatar">${sender === "user" ? "👤" : "🤖"}</div>
            <div class="chat-text">
                <div class="chat-body-content">${formattedHtml}</div>
                ${actionBtnHtml}
                <div style="display:flex;justify-content:space-between;align-items:center;margin-top:4px;">
                    <div class="chat-timestamp">${new Date().toLocaleTimeString([], { hour:'2-digit', minute:'2-digit' })}</div>
                    ${botToolsHtml}
                </div>
            </div>
        </div>
    `;

    // Append to both full-page and floating chat
    ["fullChatMessages", "floatChatMessages"].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.insertAdjacentHTML("beforeend", msgHtml);
            el.scrollTop = el.scrollHeight;
        }
    });
}

function showTypingIndicator() {
    const indicatorHtml = `
        <div class="chat-bubble bot typing" id="chatTyping">
            <div class="chat-avatar">🤖</div>
            <div class="chat-text">
                <div class="typing-dots"><span></span><span></span><span></span></div>
            </div>
        </div>
    `;
    ["fullChatMessages", "floatChatMessages"].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.insertAdjacentHTML("beforeend", indicatorHtml);
            el.scrollTop = el.scrollHeight;
        }
    });
}

function removeTypingIndicator() {
    document.querySelectorAll("#chatTyping").forEach(el => el.remove());
}

function executeChatAction(type, payload) {
    if (type === "SWITCH_TAB") {
        if (typeof switchTab === "function") {
            switchTab(payload);
            if (payload === "growth" && typeof loadGrowthAnalytics === "function") {
                loadGrowthAnalytics();
            }
        }
    } else if (type === "SELECT_SITE") {
        if (typeof switchTab === "function") {
            switchTab("chart");
        }
        const siteSelect = document.getElementById("owningSite");
        if (siteSelect) {
            for (let i = 0; i < siteSelect.options.length; i++) {
                if (siteSelect.options[i].value.toLowerCase().includes(payload.toLowerCase())) {
                    siteSelect.selectedIndex = i;
                    if (typeof onSiteChange === "function") onSiteChange();
                    break;
                }
            }
    } else if (type === "EXPORT_REPORT") {
        if (typeof downloadReport === "function") {
            downloadReport();
        } else {
            if (typeof switchTab === "function") switchTab("chart");
            setTimeout(() => { if (typeof downloadReport === "function") downloadReport(); }, 300);
        }
    }
}

function clearChatHistory() {
    chatHistory = [];
    ["fullChatMessages", "floatChatMessages"].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.innerHTML = `
                <div class="chat-bubble bot">
                    <div class="chat-avatar">🤖</div>
                    <div class="chat-text">
                        <div class="chat-body-content">
                            <strong>Conversation reset.</strong> How can I assist you with your Documentum D2 analytics today?
                        </div>
                    </div>
                </div>
            `;
        }
    });
    loadChatSuggestions();
}

function exportChat() {
    if (chatHistory.length === 0) {
        alert("No chat messages to export yet.");
        return;
    }
    let content = "# D2 Organic Growth Report — AI Chat Transcript\n";
    content += "Generated: " + new Date().toLocaleString() + "\n\n---\n\n";
    chatHistory.forEach(m => {
        content += `### ${m.sender === "user" ? "User" : "D2 Analytics Assistant"}:\n${m.text}\n\n`;
    });

    const blob = new Blob([content], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `D2_Chat_Transcript_${new Date().toISOString().slice(0,10)}.md`;
    a.click();
    URL.revokeObjectURL(url);
}

// Floating Chat Widget Toggle
function toggleFloatingChat() {
    const drawer = document.getElementById("floatingChatDrawer");
    if (drawer) {
        drawer.classList.toggle("open");
        if (drawer.classList.contains("open")) {
            const input = document.getElementById("floatChatInput");
            if (input) setTimeout(() => input.focus(), 150);
            const messages = document.getElementById("floatChatMessages");
            if (messages) messages.scrollTop = messages.scrollHeight;
        }
    }
}

// ── Lightweight Markdown to HTML Parser ──────────────────────────────────────
function parseMarkdown(md) {
    if (!md) return "";
    let html = md;

    // Escape basic raw HTML
    html = html.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

    // Headers
    html = html.replace(/^### (.*$)/gim, '<h4 class="chat-h4">$1</h4>');
    html = html.replace(/^## (.*$)/gim, '<h3 class="chat-h3">$1</h3>');
    html = html.replace(/^# (.*$)/gim, '<h2 class="chat-h2">$1</h2>');

    // Bold & Italics
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
    html = html.replace(/`(.*?)`/g, '<code class="chat-code">$1</code>');

    // Blockquotes
    html = html.replace(/^> (.*$)/gim, '<blockquote class="chat-quote">$1</blockquote>');

    // Tables
    const tableRegex = /\|(.+)\|[\r\n]+\|([-:| ]+)\|[\r\n]+((?:\|.*\|[\r\n]*)+)/g;
    html = html.replace(tableRegex, (match, header, divider, rows) => {
        const ths = header.split('|').map(h => h.trim()).filter(h => h).map(h => `<th>${h}</th>`).join('');
        const trs = rows.trim().split('\n').map(row => {
            const tds = row.split('|').map(d => d.trim()).filter(d => d).map(d => `<td>${d}</td>`).join('');
            return `<tr>${tds}</tr>`;
        }).join('');
        return `<div class="chat-table-wrap"><table class="chat-table"><thead><tr>${ths}</tr></thead><tbody>${trs}</tbody></table></div>`;
    });

    // Lists
    html = html.replace(/^\- (.*$)/gim, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>)/gims, '<ul class="chat-list">$1</ul>');

    // Paragraph line breaks
    html = html.replace(/\n\n/g, '<br><br>');
    html = html.replace(/\n/g, '<br>');

    return html;
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}

function copyChatBubble(btn) {
    const bubble = btn.closest(".chat-text");
    if (!bubble) return;
    const content = bubble.querySelector(".chat-body-content");
    if (!content) return;
    navigator.clipboard.writeText(content.innerText || content.textContent).then(() => {
        const orig = btn.textContent;
        btn.textContent = "✅ Copied!";
        setTimeout(() => { btn.textContent = orig; }, 1800);
    }).catch(() => {});
}

function speakChatBubble(btn) {
    if (!('speechSynthesis' in window)) {
        alert("Text-to-speech is not supported in this browser.");
        return;
    }
    if (window.speechSynthesis.speaking) {
        window.speechSynthesis.cancel();
        btn.textContent = "🔊 Listen";
        return;
    }
    const bubble = btn.closest(".chat-text");
    if (!bubble) return;
    const content = bubble.querySelector(".chat-body-content");
    if (!content) return;
    const text = (content.innerText || content.textContent).replace(/[*#_`|]/g, " ");
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.0;
    btn.textContent = "⏹️ Stop";
    utterance.onend = () => { btn.textContent = "🔊 Listen"; };
    utterance.onerror = () => { btn.textContent = "🔊 Listen"; };
    window.speechSynthesis.speak(utterance);
}

