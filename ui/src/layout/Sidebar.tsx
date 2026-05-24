import React from "react";

interface SidebarProps {
  children: React.ReactNode;
}

const Sidebar: React.FC<SidebarProps> = ({ children }) => {
  return (
    <div
      className="h-full bg-white dark:bg-[#090d17]/90 border-r border-gray-100 dark:border-[rgba(0,229,255,0.2)] flex flex-col transition-colors duration-300"
      style={{
        width: "300px",
        minWidth: "300px",
      }}
    >
      {children}
    </div>
  );
};

export default Sidebar;
