import React, { useState, useEffect } from "react";
import axios from "axios";
import "./App.css";

function App() {
  const [file, setFile] = useState(null);
  const [downloadUrl, setDownloadUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [watermarkText, setWatermarkText] = useState("");
  const [decryptFile, setDecryptFile] = useState(null);
  const [decryptedWatermark, setDecryptedWatermark] = useState("");
  const [darkMode, setDarkMode] = useState(false);

  useEffect(() => {
    document.body.className = darkMode ? "dark" : "";
  }, [darkMode]);

  const toggleDarkMode = () => setDarkMode((prev) => !prev);

  const handleFileChange = (event) => {
    setFile(event.target.files[0]);
  };

  const handleWatermarkChange = (event) => {
    setWatermarkText(event.target.value);
  };

  const handleUpload = async () => {
    if (!file || !watermarkText.trim()) {
      alert("Please select a file and enter watermark text!");
      return;
    }

    setLoading(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("watermark", watermarkText);

    try {
      const response = await axios.post(
        "http://localhost:8080/api/files/upload",
        formData,
        {
          responseType: "blob",
          headers: { "Content-Type": "multipart/form-data" },
        }
      );

      const url = window.URL.createObjectURL(new Blob([response.data]));
      setDownloadUrl(url);
    } catch (error) {
      console.error("Error uploading file:", error);
      alert("File upload failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleDecryptChange = (event) => {
    setDecryptFile(event.target.files[0]);
  };

  const handleDecrypt = async () => {
    if (!decryptFile) {
      alert("Please upload a file to decrypt.");
      return;
    }

    const formData = new FormData();
    formData.append("file", decryptFile);

    try {
      const response = await axios.post(
        "http://localhost:8080/api/files/decrypt",
        formData,
        { headers: { "Content-Type": "multipart/form-data" } }
      );
      setDecryptedWatermark(response.data);
    } catch (error) {
      console.error("Decryption error:", error);
      setDecryptedWatermark("Failed to decrypt watermark.");
    }
  };

  return (
    <>
      <nav className="navbar">
        <h1>Watermark Encryption Website</h1>
        <button onClick={toggleDarkMode} className="dark-toggle">
          {darkMode ? "🌞 Light Mode" : "🌙 Dark Mode"}
        </button>
      </nav>

      <div className="container">
        {/* Upload Section */}
        <div className="card">
          <h2>Upload a DOCX File</h2>
          <input type="file" accept=".docx" onChange={handleFileChange} />
          <input
            type="text"
            placeholder="Enter watermark text"
            value={watermarkText}
            onChange={handleWatermarkChange}
          />
          <button onClick={handleUpload} disabled={loading}>
            {loading ? "Uploading..." : "Upload & Apply Watermark"}
          </button>
          {loading && <div className="loader"></div>}
          {downloadUrl && (
            <a href={downloadUrl} download="watermarked.docx">
              <button className="download-btn">Download Watermarked File</button>
            </a>
          )}
        </div>

        {/* Decryption Section */}
        <div className="card">
          <h2>Decrypt Watermark from DOCX</h2>
          <input type="file" accept=".docx" onChange={handleDecryptChange} />
          <button onClick={handleDecrypt}>Decrypt Watermark</button>
          {decryptedWatermark && (
            <div className="result">
              <h4>Decrypted Watermark:</h4>
              <p>{decryptedWatermark}</p>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

export default App;
