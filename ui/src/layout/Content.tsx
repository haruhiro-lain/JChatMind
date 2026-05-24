import React from "react";

interface ContentProps {
  children: React.ReactNode;
}

const Content: React.FC<ContentProps> = ({ children }) => {
  return (
    <div className="h-full flex-1 bg-[#fafbfc] dark:bg-transparent flex flex-col min-w-0 transition-colors duration-300">
      {children}
    </div>
  );
};

export default Content;
